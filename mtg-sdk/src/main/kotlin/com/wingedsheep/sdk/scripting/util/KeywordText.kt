package com.wingedsheep.sdk.scripting.util

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword

/**
 * Player-visible wording for granting or removing a keyword-or-flag, as stored on the grant
 * effects and statics (the enum *name* — `"FIRST_STRIKE"`, `"CANT_BE_BLOCKED"`).
 *
 * A [Keyword] is a noun the subject *gains* ("target creature gains first strike"). Most
 * [AbilityFlag]s are not: their display names are verb phrases ("Can't be blocked", "Assigns
 * combat damage equal to its toughness rather than its power"), so the subject takes the phrase
 * directly — "target creature can't be blocked", never "target creature gains can't be blocked".
 * The few flags that aren't predicates of their subject ("You may choose not to untap") are
 * quoted as a granted ability instead.
 *
 * @param plural the subject is plural ("creatures you control"), so the verb agrees with it.
 */
fun describeKeywordGrant(subject: String, keyword: String, plural: Boolean = false): String {
    Keyword.entries.firstOrNull { it.name == keyword }?.let {
        return "$subject ${if (plural) "gain" else "gains"} ${it.displayName.lowercase()}"
    }
    val flag = AbilityFlag.entries.firstOrNull { it.name == keyword }
        ?: return "$subject ${if (plural) "gain" else "gains"} ${keyword.lowercase().replace('_', ' ')}"
    val predicate = flagPredicate(flag, plural)
        ?: return "$subject ${if (plural) "gain" else "gains"} \"${flag.displayName}\""
    return "$subject $predicate"
}

/**
 * The inverse of [describeKeywordGrant]: "target creature loses flying". A flag is quoted, since
 * its display name is a predicate that can't follow "loses".
 */
fun describeKeywordLoss(subject: String, keyword: String, plural: Boolean = false): String {
    val verb = if (plural) "lose" else "loses"
    Keyword.entries.firstOrNull { it.name == keyword }?.let {
        return "$subject $verb ${it.displayName.lowercase()}"
    }
    AbilityFlag.entries.firstOrNull { it.name == keyword }?.let {
        return "$subject $verb \"${it.displayName.replaceFirstChar { c -> c.lowercase() }}\""
    }
    return "$subject $verb ${keyword.lowercase().replace('_', ' ')}"
}

/**
 * The lower-case display text of a keyword-or-flag name, for list contexts that only need the
 * phrase ("flying", "can't be blocked").
 */
fun keywordDisplayText(keyword: String): String =
    Keyword.entries.firstOrNull { it.name == keyword }?.displayName?.lowercase()
        ?: AbilityFlag.entries.firstOrNull { it.name == keyword }?.displayName?.replaceFirstChar { it.lowercase() }
        ?: keyword.lowercase().replace('_', ' ')

/**
 * [flag]'s display name as a predicate of its subject, or null when it isn't one. Display names
 * are written for a singular subject; [plural] conjugates the leading verb.
 */
private fun flagPredicate(flag: AbilityFlag, plural: Boolean): String? {
    val text = flag.displayName.replaceFirstChar { it.lowercase() }
    val first = text.substringBefore(' ')
    if (first !in SINGULAR_TO_PLURAL_VERB) return null
    return if (plural) SINGULAR_TO_PLURAL_VERB.getValue(first) + text.removePrefix(first) else text
}

/** The leading verbs of the predicate-shaped flag names, with their plural forms. */
private val SINGULAR_TO_PLURAL_VERB = mapOf(
    "can't" to "can't",
    "doesn't" to "don't",
    "isn't" to "aren't",
    "assigns" to "assign",
)
