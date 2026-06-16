import json
import os
import calendar
from datetime import date, timedelta


def subtract_months(d, n):
    m = d.month - n
    y = d.year
    while m <= 0:
        m += 12
        y -= 1
    return date(y, m, min(d.day, calendar.monthrange(y, m)[1]))


ref_str = os.environ.get("REFERENCE_DATE", "").strip()
today = date.fromisoformat(ref_str) if ref_str else date.today()
window_start = subtract_months(today, 3)

# 2 categories of each type:
#   default        - Symptoms (system), Mood (custom)
#   numeric_slider - Flow (system), Brain Fog (custom)
#   increment      - Pads Used (custom), Pads bled through (custom)
#   numeric_free   - Outbursts (custom), Energy (custom)
categories = [
    {
        "id": 1, "name": "Flow", "isSystem": True, "systemKey": "flow",
        "displayOrder": 0, "iconName": "water", "colorToken": "primary",
        "categoryType": "numeric_slider",
        "numericMin": 1, "numericMax": 4, "allowDecimals": False,
        "numericUnit": "",
        "scaleLabels": "1=Spotting\n2=Light\n3=Medium\n4=Heavy",
        "isArchived": False, "allowMultiple": False,
        "showInLogPeriod": False, "trackAgainstTime": True,
        "values": ["Spotting", "Light", "Medium", "Heavy"],
    },
    {
        "id": 2, "name": "Symptoms", "isSystem": True, "systemKey": "symptoms",
        "displayOrder": 1, "iconName": "meditation", "colorToken": "secondary",
        "categoryType": "default",
        "numericMin": 0, "numericMax": 10, "allowDecimals": False,
        "numericUnit": "", "scaleLabels": "",
        "isArchived": False, "allowMultiple": False,
        "showInLogPeriod": False, "trackAgainstTime": False,
        "values": [
            "Cramps", "Headache", "Bloating",
            "Fatigue", "Back Pain", "Bleeding (non-period)",
        ],
    },
    {
        "id": 3, "name": "Mood", "isSystem": False, "systemKey": "",
        "displayOrder": 2, "iconName": "psychology", "colorToken": "tertiary",
        "categoryType": "default",
        "numericMin": 0, "numericMax": 10, "allowDecimals": False,
        "numericUnit": "", "scaleLabels": "",
        "isArchived": False, "allowMultiple": True,
        "showInLogPeriod": True, "trackAgainstTime": True,
        "values": ["Happy", "Sad", "Anxious", "Calm", "Irritable", "Content", "Grumpy"],
    },
    {
        "id": 6, "name": "Pads Used", "isSystem": False, "systemKey": "",
        "displayOrder": 3, "iconName": "healing", "colorToken": "FFF7A7DD",
        "categoryType": "increment",
        "numericMin": 0, "numericMax": 10, "allowDecimals": False,
        "numericUnit": "", "scaleLabels": "",
        "isArchived": False, "allowMultiple": False,
        "showInLogPeriod": True, "trackAgainstTime": True,
        "values": [],
    },
    {
        "id": 7, "name": "Outbursts", "isSystem": False, "systemKey": "",
        "displayOrder": 4, "iconName": "bolt", "colorToken": "FFE53935",
        "categoryType": "numeric_free",
        "numericMin": 0, "numericMax": 20, "allowDecimals": False,
        "numericUnit": "explosions", "scaleLabels": "",
        "isArchived": False, "allowMultiple": True,
        "showInLogPeriod": True, "trackAgainstTime": True,
        "values": [],
    },
    {
        "id": 8, "name": "Pads bled through", "isSystem": False, "systemKey": "",
        "displayOrder": 5, "iconName": "medication", "colorToken": "secondary",
        "categoryType": "increment",
        "numericMin": 0, "numericMax": 10, "allowDecimals": False,
        "numericUnit": "", "scaleLabels": "",
        "isArchived": False, "allowMultiple": False,
        "showInLogPeriod": True, "trackAgainstTime": False,
        "values": [],
    },
    {
        "id": 10, "name": "Brain Fog", "isSystem": False, "systemKey": "",
        "displayOrder": 6, "iconName": "cloud", "colorToken": "FF8A8A8A",
        "categoryType": "numeric_slider",
        "numericMin": 1, "numericMax": 3, "allowDecimals": False,
        "numericUnit": "",
        "scaleLabels": "1=Good\n2=Confused\n3=What's my name again?",
        "isArchived": False, "allowMultiple": True,
        "showInLogPeriod": True, "trackAgainstTime": True,
        "values": [],
    },
    {
        "id": 11, "name": "Energy", "isSystem": False, "systemKey": "",
        "displayOrder": 7, "iconName": "fitness", "colorToken": "FFFF9800",
        "categoryType": "numeric_free",
        "numericMin": 0, "numericMax": 100, "allowDecimals": False,
        "numericUnit": "%", "scaleLabels": "",
        "isArchived": False, "allowMultiple": False,
        "showInLogPeriod": True, "trackAgainstTime": True,
        "values": [],
    },
]

# Cycle specs: (cycle_length, period_duration, overall_flow, symptoms)
cycle_specs = [
    (29, 6, "Heavy",  ["Cramps", "Headache"]),
    (28, 5, "Medium", ["Cramps", "Bloating", "Fatigue"]),
    (31, 7, "Heavy",  ["Back Pain", "Headache", "Bloating"]),
    (27, 4, "Light",  ["Fatigue"]),
]

# Flow value by day-of-period (numeric string "1"-"4")
flow_by_day = {1: "4", 2: "4", 3: "4", 4: "3", 5: "2", 6: "1", 7: "1"}
pads_by_flow = {"4": 4, "3": 3, "2": 2, "1": 1}
bled_by_flow = {"4": 2, "3": 1, "2": 0, "1": 0}

periods = []
period_days = set()
tracking_logs = {cat["id"]: [] for cat in categories}

period_start = window_start
spec_idx = 0
pid = 1

while period_start <= today:
    cycle_len, duration, flow_label, symptoms = cycle_specs[spec_idx % len(cycle_specs)]
    period_end_d = period_start + timedelta(days=duration - 1)
    ongoing = period_end_d > today

    periods.append({
        "id": pid,
        "startDate": period_start.isoformat(),
        "endDate": None if ongoing else period_end_d.isoformat(),
        "flowLevel": flow_label,
        "notes": "",
        "symptoms": symptoms,
    })

    log_end = today if ongoing else period_end_d
    d = period_start
    day_num = 1
    while d <= log_end:
        period_days.add(d)
        flow_val = flow_by_day.get(day_num, "1")

        tracking_logs[1].append({"date": d.isoformat(), "values": [flow_val], "notes": ""})

        base = pads_by_flow.get(flow_val, 1)
        pads = max(0, base + (day_num % 3) - 1)
        tracking_logs[6].append({"date": d.isoformat(), "values": [str(pads)], "notes": ""})

        bled = bled_by_flow.get(flow_val, 0)
        if day_num % 2 == 0:
            bled = max(0, bled - 1)
        tracking_logs[8].append({"date": d.isoformat(), "values": [str(bled)], "notes": ""})

        d += timedelta(days=1)
        day_num += 1

    pid += 1
    spec_idx += 1
    period_start += timedelta(days=cycle_len)

# Symptoms: pre-menstrual (3 days before) + first 3 period days
for p in periods:
    ps = date.fromisoformat(p["startDate"])
    pe = date.fromisoformat(p["endDate"]) if p["endDate"] else today

    for offset in (3, 2, 1):
        pms = ps - timedelta(days=offset)
        if pms >= window_start:
            tracking_logs[2].append({
                "date": pms.isoformat(),
                "values": ["Bloating", "Cramps"],
                "notes": "",
            })

    syms = p["symptoms"]
    d = ps
    for day_num in range(1, 4):
        if d > pe:
            break
        tracking_logs[2].append({
            "date": d.isoformat(),
            "values": syms[:max(1, len(syms) - day_num + 1)],
            "notes": "",
        })
        d += timedelta(days=1)

# Mood: every other day, multi-value during period
single_moods = ["Happy", "Calm", "Content", "Sad", "Anxious", "Irritable", "Grumpy"]
multi_moods = [
    ["Happy", "Calm"],
    ["Sad", "Anxious"],
    ["Irritable", "Grumpy"],
    ["Content", "Happy"],
]
d = window_start
idx = 0
while d <= today:
    if idx % 2 == 0:
        if d in period_days:
            moods = multi_moods[2] if idx % 4 < 2 else ["Grumpy"]
        elif idx % 7 < 2:
            moods = multi_moods[0]
        elif idx % 7 < 4:
            moods = [single_moods[idx % len(single_moods)]]
        else:
            moods = multi_moods[idx % len(multi_moods)]
        tracking_logs[3].append({"date": d.isoformat(), "values": moods, "notes": ""})
    d += timedelta(days=1)
    idx += 1

# Outbursts: higher counts on period days, low outside
d = window_start
idx = 0
while d <= today:
    if d in period_days and idx % 2 == 0:
        val = 3 + (idx % 5)
        tracking_logs[7].append({"date": d.isoformat(), "values": [str(val)], "notes": ""})
    elif idx % 5 == 0 and d not in period_days:
        val = idx % 3
        tracking_logs[7].append({"date": d.isoformat(), "values": [str(val)], "notes": ""})
    d += timedelta(days=1)
    idx += 1

# Brain Fog: every 3 days, max during period
d = window_start
idx = 0
while d <= today:
    if idx % 3 == 0:
        if d in period_days:
            fog = "3"
        elif idx % 9 < 3:
            fog = "1"
        elif idx % 9 < 6:
            fog = "2"
        else:
            fog = "3"
        tracking_logs[10].append({"date": d.isoformat(), "values": [fog], "notes": ""})
    d += timedelta(days=1)
    idx += 1

# Energy: daily, dips during period, peaks mid-cycle
d = window_start
idx = 0
while d <= today:
    if d in period_days:
        energy = 20 + (idx % 20)
    elif idx % 14 in (6, 7):
        energy = 85 + (idx % 15)
    else:
        energy = 50 + (idx % 35)
    energy = min(energy, 100)
    tracking_logs[11].append({"date": d.isoformat(), "values": [str(energy)], "notes": ""})
    d += timedelta(days=1)
    idx += 1

cat_by_id = {c["id"]: c for c in categories}
tracking = [
    {
        "id": cid,
        "name": cat_by_id[cid]["name"],
        "archived": False,
        "type": cat_by_id[cid]["categoryType"],
        "logs": logs,
    }
    for cid, logs in tracking_logs.items()
    if logs
]

pinned = [
    {
        "id": "1__TIME_SERIES_MONTH",
        "label": "Flow",
        "categoryId1": 1,
        "timeRangeType": "MONTH",
        "chartType": "TIME_SERIES",
    },
    {
        "id": "2__PIE_ALL_TIME",
        "label": "Symptoms",
        "categoryId1": 2,
        "timeRangeType": "ALL_TIME",
        "chartType": "PIE",
    },
    {
        "id": "6_8_DUAL_TIME_SERIES_MONTH",
        "label": "Pads Used vs Pads bled through",
        "categoryId1": 6,
        "categoryId2": 8,
        "timeRangeType": "MONTH",
        "chartType": "DUAL_TIME_SERIES",
    },
    {
        "id": "11__TIME_SERIES_MONTH",
        "label": "Energy",
        "categoryId1": 11,
        "timeRangeType": "MONTH",
        "chartType": "TIME_SERIES",
    },
]

export = {
    "version": 3,
    "exportedAt": today.isoformat(),
    "fullBackup": True,
    "dateRange": {
        "from": window_start.isoformat(),
        "to": today.isoformat(),
    },
    "categories": categories,
    "pinnedStats": json.dumps(pinned),
    "periods": periods,
    "tracking": tracking,
}

filename = f"goflo_test_data_{today.isoformat()}.json"
with open(filename, "w") as fh:
    json.dump(export, fh, indent=2)

print(f"Window : {window_start} to {today}")
print(f"Periods: {len(periods)}")
for cat in categories:
    logs = tracking_logs[cat["id"]]
    print(f"  [{cat['categoryType']:<16}] {cat['name']:<20}  {len(logs):3d} logs")
print(f"Output : {filename}")
