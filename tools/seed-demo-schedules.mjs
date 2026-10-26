#!/usr/bin/env node
/**
 * Seeds a demo account with ~12 habits and a schedule for each, so Home has a full list today.
 *
 *   node tools/seed-demo-schedules.mjs [--email demo@example.com] [--password demo1234]
 *                                      [--username demo] [--base-url http://localhost:8080]
 *
 * Re-runnable: habits are matched by name (case-insensitive) and skipped if they exist; a schedule
 * is only created for a habit that has none yet (covers a run that died between the two calls).
 *
 * Backend facts this relies on (src/schedule/schedule.service.ts):
 * - Routes have no /api prefix. Categories are seeded by main.ts on boot; no endpoint creates them.
 * - POST /schedule/recurring only knows repeatPattern none|daily|weekdays|weekends. repeatDays is how
 *   many days ahead (from the server's "today") to expand into one Schedule row per matching day.
 *   Only the hour/minute of start_time is used, in the server's time zone (UTC in Docker).
 * - "Weekly" = POST /schedule/recurring/weekdays. It compares daysOfWeek with JS getDay()
 *   (0 = Sunday), despite its enum documenting Sunday as 7, so we send getDay() values.
 * - There is no monthly pattern, so "monthly" = one POST /schedule/custom per month.
 * - /schedule/day?date=YYYY-MM-DD matches the stored `date` column (UTC midnight of the generation
 *   day). Start times are kept between 06:00 and 22:00 local so their UTC date equals the local date.
 */

const args = parseArgs(process.argv.slice(2));
const BASE_URL = (args['base-url'] ?? 'http://localhost:8080').replace(/\/+$/, '');
const EMAIL = args.email ?? 'demo@example.com';
const PASSWORD = args.password ?? 'demo1234';
const USERNAME = args.username ?? EMAIL.split('@')[0];

const DAILY_DAYS = 30;
const WEEKLY_WEEKS = 8;
const MONTHLY_MONTHS = 3;

// time = local HH:MM today. status (optional) is applied to today's occurrence if it is already past.
const HABITS = [
  { name: 'Morning Hydration', category: 'Hydration', description: 'A big glass of water right after waking up', goal: 'Drink 2 litres of water every day', repeat: 'daily', time: '06:45', minutes: 5, notes: 'Before coffee!', status: 'Completed' },
  { name: 'Morning Run', category: 'Running', description: 'Easy 5 km around the park', goal: 'Run a half marathon in spring', repeat: 'weekdays', time: '07:15', minutes: 35, notes: 'Keep the pace conversational', status: 'Completed' },
  { name: 'Stretch & Mobility', category: 'Exercise', description: 'Full-body stretching routine', goal: 'Touch my toes by December', repeat: 'daily', time: '08:00', minutes: 15, status: 'Skipped' },
  { name: 'Deep Work: Algorithms', category: 'Study', description: 'Focused practice on algorithm problems', goal: 'Solve 100 LeetCode problems this semester', repeat: 'weekdays', time: '09:00', minutes: 90, notes: 'Phone in another room' },
  { name: 'Read a Tech Article', category: 'Reading', description: 'One in-depth article about Android or Kotlin', goal: 'Read 60 articles in two months', repeat: 'daily', time: '12:30', minutes: 20 },
  { name: 'Midday Water Check', category: 'Hydration', description: 'Refill the bottle and drink half of it', goal: 'Never skip the afternoon refill', repeat: 'daily', time: '13:30', minutes: 5 },
  { name: 'Android Lab Prep', category: 'Study', description: 'Review the lab sheet and prepare the project', goal: 'Finish every lab before the deadline', repeat: 'weekly', time: '15:00', minutes: 60, notes: 'Check the backend source, not only the PDF' },
  { name: 'Gym Strength Session', category: 'Exercise', description: 'Squats, bench press and rows', goal: 'Bench press 80 kg by summer', repeat: 'weekly', time: '17:30', minutes: 60 },
  { name: 'Monthly Budget Review', category: 'Other', description: 'Go through expenses and plan next month', goal: 'Save 15% of my income', repeat: 'monthly', time: '19:00', minutes: 45, notes: 'Export the bank statement first' },
  { name: 'Blog Post Draft', category: 'Writing', description: 'Write the first draft of a blog post', goal: 'Publish one post every month', repeat: 'monthly', time: '20:00', minutes: 60 },
  { name: 'Evening Journal', category: 'Writing', description: 'Three things that went well today', goal: 'Journal 30 days in a row', repeat: 'daily', time: '21:15', minutes: 15 },
  { name: 'Evening Reading', category: 'Reading', description: 'Fiction before bed, no screens', goal: 'Read a book every two weeks', repeat: 'daily', time: '21:45', minutes: 30, notes: 'Reading improves your brain activity' },
];

main().catch((err) => {
  console.error(`\nSeeding failed: ${err.message}`);
  process.exit(1);
});

async function main() {
  console.log(`Backend: ${BASE_URL}`);
  const token = await loginOrSignup();

  const categories = await api('GET', '/habit/categories', { token });
  const categoryByName = new Map(categories.map((c) => [c.name.toLowerCase(), c]));
  const fallbackCategory = categoryByName.get('other') ?? categories[0];
  if (!fallbackCategory) throw new Error('The backend has no habit categories');

  const existingHabits = await api('GET', '/habit', { token });
  const habitByName = new Map(existingHabits.map((h) => [h.name.toLowerCase(), h]));
  const allSchedules = await api('GET', '/schedule', { token });
  const habitIdsWithSchedules = new Set(allSchedules.map((s) => s.habit?.id));

  let createdHabits = 0;
  let createdSchedules = 0;
  for (const spec of HABITS) {
    let habit = habitByName.get(spec.name.toLowerCase());
    if (habit) {
      console.log(`= ${spec.name}: habit exists (#${habit.id})`);
    } else {
      const category = categoryByName.get(spec.category.toLowerCase()) ?? fallbackCategory;
      habit = await api('POST', '/habit', {
        token,
        body: { name: spec.name, description: spec.description, categoryId: category.id, goal: spec.goal },
      });
      createdHabits++;
      console.log(`+ ${spec.name}: habit #${habit.id} (${category.name})`);
    }

    if (habitIdsWithSchedules.has(habit.id)) continue;
    const rows = await createSchedules(token, habit.id, spec);
    createdSchedules += rows.length;
    console.log(`  + ${rows.length} ${spec.repeat} schedule row(s) at ${spec.time} local`);
    await applyTodayStatus(token, rows, spec);
  }

  console.log(`\nCreated ${createdHabits} habit(s) and ${createdSchedules} schedule row(s).`);

  const today = localDateString(new Date());
  const todays = await api('GET', `/schedule/day?date=${today}`, { token });
  console.log(`\nGET /schedule/day?date=${today} -> ${todays.length} schedule(s):`);
  todays
    .sort((a, b) => new Date(a.start_time) - new Date(b.start_time))
    .forEach((s) =>
      console.log(
        `  ${localTime(s.start_time)}-${localTime(s.end_time)}  ${s.habit?.name} ` +
          `[${s.habit?.category?.name}] ${s.status}${s.notes ? ` - ${s.notes}` : ''}`,
      ),
    );
}

async function loginOrSignup() {
  try {
    const res = await api('POST', '/auth/local/signin', { body: { email: EMAIL, password: PASSWORD } });
    console.log(`Logged in as ${EMAIL}`);
    return res.tokens.accessToken;
  } catch (err) {
    // Unknown email and wrong password are both 403 "Access denied"; signup tells them apart.
    if (err.status !== 403) throw err;
  }
  const form = new FormData();
  form.append('username', USERNAME);
  form.append('email', EMAIL);
  form.append('password', PASSWORD);
  try {
    const res = await api('POST', '/auth/local/signup', { form });
    console.log(`Signed up ${EMAIL} (username "${USERNAME}")`);
    return res.tokens.accessToken;
  } catch (err) {
    if (/duplicate key/i.test(err.message)) {
      throw new Error(`${EMAIL} (or username "${USERNAME}") already exists but the password was rejected`);
    }
    throw err;
  }
}

/** Creates the schedule rows for one habit and returns them. */
async function createSchedules(token, habitId, spec) {
  const now = new Date();
  const start = atLocalTime(now, spec.time);
  const common = { habitId, start_time: start.toISOString(), duration_minutes: spec.minutes, participantIds: [] };
  if (spec.notes) common.notes = spec.notes;

  switch (spec.repeat) {
    case 'daily':
    case 'weekdays':
    case 'weekends':
      return api('POST', '/schedule/recurring', {
        token,
        body: { ...common, repeatPattern: spec.repeat, repeatDays: DAILY_DAYS, is_custom: false },
      });
    case 'weekly':
      return api('POST', '/schedule/recurring/weekdays', {
        token,
        body: { ...common, daysOfWeek: [now.getDay()], numberOfWeeks: WEEKLY_WEEKS },
      });
    case 'monthly': {
      const rows = [];
      for (let i = 0; i < MONTHLY_MONTHS; i++) {
        const day = addMonthsClamped(now, i);
        const startOnDay = atLocalTime(day, spec.time);
        rows.push(
          await api('POST', '/schedule/custom', {
            token,
            body: { ...common, date: localDateString(day), start_time: startOnDay.toISOString(), is_custom: true },
          }),
        );
      }
      return rows;
    }
    default:
      throw new Error(`Unknown repeat "${spec.repeat}" for ${spec.name}`);
  }
}

/** Gives the list some status variety: marks today's row Completed/Skipped if its time has passed. */
async function applyTodayStatus(token, rows, spec) {
  if (!spec.status) return;
  const today = localDateString(new Date());
  const row = rows.find((r) => localDateString(new Date(r.start_time)) === today);
  if (!row || new Date(row.end_time) > new Date()) return;
  await api('PATCH', `/schedule/${row.id}`, { token, body: { status: spec.status } });
  console.log(`  ~ today's occurrence marked ${spec.status}`);
}

async function api(method, path, { token, body, form } = {}) {
  const headers = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body) headers['Content-Type'] = 'application/json';
  let res;
  try {
    res = await fetch(BASE_URL + path, { method, headers, body: form ?? (body && JSON.stringify(body)) });
  } catch (err) {
    throw new Error(`${method} ${path}: cannot reach ${BASE_URL} (${err.cause?.code ?? err.message}). Is the backend running?`);
  }
  const text = await res.text();
  const data = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const message = Array.isArray(data?.message) ? data.message.join('; ') : data?.message ?? text;
    const error = new Error(`${method} ${path} -> ${res.status}: ${message}`);
    error.status = res.status;
    throw error;
  }
  return data;
}

function atLocalTime(day, hhmm) {
  const [h, m] = hhmm.split(':').map(Number);
  return new Date(day.getFullYear(), day.getMonth(), day.getDate(), h, m, 0, 0);
}

function addMonthsClamped(date, months) {
  const target = new Date(date.getFullYear(), date.getMonth() + months, 1);
  const lastDay = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate();
  target.setDate(Math.min(date.getDate(), lastDay));
  return target;
}

function localDateString(d) {
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

function localTime(iso) {
  return iso ? new Date(iso).toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' }) : '--:--';
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const m = argv[i].match(/^--([^=]+)(?:=(.*))?$/);
    if (!m) continue;
    out[m[1]] = m[2] ?? argv[++i];
  }
  return out;
}
