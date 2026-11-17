package com.fildy.data

import java.time.ZoneId

/**
 * The farm's time zone: Buenos Aires, Argentina (UTC-3, no daylight saving time). "Today",
 * "yesterday", rain dates and saved times always follow the farm's clock, not the phone's
 * setting.
 */
val FarmTimeZone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")
