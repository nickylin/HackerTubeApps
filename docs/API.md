# HackerTube API notes

Base: `https://hackertube.org` — public, no key. Verified live on 2026-11-02.

A video object contains: `vid` (YouTube id), `title`, `channel`, `published`,
`duration_s`, `views`, `likes`, `thumbnail`, `summary`, `why_hn`,
`content_type`, `level`, `shelf_life`, `primary_topic`, `tags[]`,
`categories[]` (`id`, `name`, `description`, `area`, `area_name`),
`quote` (`text`, `id`, `year`),
`hn` (`mentions`, `recommenders`, `stances`, `rec_months`, `strong`, `first`, `last`, `last_rec`),
`score`, `whys[]`, `hn_related`, `reason`, `source`.

Dataset (Karpathy, 2026-10-31): ~43M HN comments scanned -> ~39,834 recommending
comments -> 6,454 distinct videos -> 150 topics.

Endpoints are listed in the README table. Response shapes the app tolerates are
documented in `HackerTubeApi.kt`.
