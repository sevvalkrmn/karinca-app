"""Small date-arithmetic helpers shared by the monthly income and recurring
payments jobs — both need "advance to the first of next month" and "last day of
this month" for their identical catch-up-loop shape.
"""

import calendar
from datetime import date, timedelta


def add_month(d: date) -> date:
    if d.month == 12:
        return date(d.year + 1, 1, 1)
    return date(d.year, d.month + 1, 1)


def month_end(d: date) -> date:
    return add_month(d) - timedelta(days=1)


def clamp_day(month: date, day_of_month: int) -> date:
    last_day = calendar.monthrange(month.year, month.month)[1]
    return date(month.year, month.month, min(day_of_month, last_day))
