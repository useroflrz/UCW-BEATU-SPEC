package com.ucw.beatu.ai

object AiSearchComponentContract {
    const val ACTION_AI_SEARCH_RESULT = "com.ucw.beatu.action.AI_SEARCH_RESULT"
    const val ACTION_OPEN_AI_HISTORY = "com.ucw.beatu.action.OPEN_AI_SEARCH_HISTORY"
    const val ACTION_PERSIST_AI_SEARCH = "com.ucw.beatu.action.PERSIST_AI_SEARCH"
    const val ACTION_OPEN_AI_TOPIC = "com.ucw.beatu.action.OPEN_AI_TOPIC"
    const val ACTION_TRIGGER_GROWTH_ENGAGEMENT = "com.ucw.beatu.action.TRIGGER_GROWTH_ENGAGEMENT"
    const val ACTION_BOOTSTRAP_GROWTH = "com.ucw.beatu.action.BOOTSTRAP_GROWTH"
    const val ACTION_INTERNAL_GROWTH_PUSH = "com.ucw.beatu.action.INTERNAL_GROWTH_PUSH"

    const val EXTRA_QUERY = "extra_query"
    const val EXTRA_ANSWER = "extra_answer"
    const val EXTRA_KEYWORDS = "extra_keywords"
    const val EXTRA_SOURCE = "extra_source"
    const val EXTRA_TRIGGER = "extra_trigger"

    const val AUTHORITY_SUFFIX = ".ai.search.provider"
    const val PATH_HISTORY = "history"
    const val PATH_SHARE_HOT = "share_hot_topics"
}

