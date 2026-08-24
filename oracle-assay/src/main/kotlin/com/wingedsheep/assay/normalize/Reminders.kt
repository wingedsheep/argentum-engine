package com.wingedsheep.assay.normalize

import com.wingedsheep.assay.syntax.SentenceCase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Reminder text, **regenerated from the model** rather than remembered.
 *
 * The design is specific about this: reminder text is stripped for parsing and regenerated from
 * the keyword when printing, and a mismatch is a finding — our keyword model and the printed gloss
 * disagree about what the keyword does. Remembering the stripped bytes instead would make the
 * pass's inverse trivially correct and prove nothing.
 *
 * So the touchstone runs on reminder-free text (where the strip is recorded and restored exactly,
 * which is what keeps *that* gate honest), and this lexicon powers a separate audit reported
 * alongside it: **matched**, **differed**, or **unglossed**. A `differed` row is worth reading; an
 * `unglossed` row is only a gap in this table.
 *
 * Two things the table makes visible that the model does not carry:
 *
 * - **The object noun.** Printed reminder text says "this creature", "this artifact", "this land"
 *   depending on the card, so a gloss is a function of the ability *and* the card's types.
 *   [gloss] takes that noun as a parameter; a [KeywordAbility] alone cannot produce it.
 * - **The article.** "controls a Swamp" but "controls an Island" — trivial, and exactly the sort of
 *   thing a hand-maintained gloss string gets wrong once and never notices.
 */
object Reminders {

    /**
     * The reminder text this ability would print on a card whose self-reference reads [self]
     * (e.g. "this creature"), or null when the table has no gloss for it.
     *
     * Glosses are written mid-sentence for the same reason templates are — most of them begin with
     * [self], whose noun is capitalized or not depending on where it lands — and sentence case is
     * applied once, here, on the way out.
     */
    fun gloss(ability: KeywordAbility, self: String = "this creature"): String? =
        rawGloss(ability, self)?.let(SentenceCase::capitalize)

    /**
     * Printed reminder text spells small numbers as words ("three time counters"), which the model
     * carries as an `Int`. Anything past ten is printed in digits on real cards too.
     */
    private fun words(n: Int): String = when (n) {
        1 -> "one"; 2 -> "two"; 3 -> "three"; 4 -> "four"; 5 -> "five"
        6 -> "six"; 7 -> "seven"; 8 -> "eight"; 9 -> "nine"; 10 -> "ten"
        else -> n.toString()
    }

    private fun rawGloss(ability: KeywordAbility, self: String): String? = when (ability) {
        is KeywordAbility.Simple -> simpleGloss(ability.keyword, self)
        is KeywordAbility.Ward -> wardGloss(ability, self)
        is KeywordAbility.Cycling -> ability.cost.let { "$it, Discard this card: Draw a card." }
            .takeIf { ability.searchFilter == null }

        is KeywordAbility.Numeric -> numericGloss(ability, self)
        is KeywordAbility.Affinity ->
            "This spell costs {1} less to cast for each ${ability.forType.displayName.lowercase()} you control."

        is KeywordAbility.AffinityForSubtype ->
            "This spell costs {1} less to cast for each ${ability.forSubtype.value} you control."

        is KeywordAbility.Flashback ->
            "You may cast this card from your graveyard for its flashback cost. Then exile it."
                .takeIf { ability.additionalCost == null }

        is KeywordAbility.Madness ->
            "If you discard this card, discard it into exile. When you do, cast it for its madness cost " +
                "or put it into your graveyard."

        is KeywordAbility.Foretell ->
            "During your turn, you may pay {2} and exile this card from your hand face down. Cast it on a later " +
                "turn for its foretell cost."

        is KeywordAbility.Miracle ->
            "You may cast this card for its miracle cost when you draw it if it's the first card you drew this turn."

        is KeywordAbility.Dash ->
            "You may cast this spell for its dash cost. If you do, it gains haste, and it's returned from the " +
                "battlefield to its owner's hand at the beginning of the next end step."

        is KeywordAbility.Evoke ->
            "You may cast this spell for its evoke cost. If you do, it's sacrificed when it enters."

        is KeywordAbility.Suspend -> suspendGloss(ability)
        else -> null
    }

    /** The self-reference noun a card's reminder text uses, derived from its printed type line. */
    fun selfNoun(typeLine: String): String = when {
        typeLine.contains("Creature") -> "this creature"
        typeLine.contains("Instant") || typeLine.contains("Sorcery") -> "this spell"
        typeLine.contains("Land") -> "this land"
        typeLine.contains("Artifact") -> "this artifact"
        typeLine.contains("Enchantment") -> "this enchantment"
        typeLine.contains("Planeswalker") -> "this planeswalker"
        typeLine.contains("Battle") -> "this battle"
        else -> "this permanent"
    }

    private fun suspendGloss(ability: KeywordAbility.Suspend): String =
        "Rather than cast this card from your hand, you may pay ${ability.cost} and exile it with " +
            "${words(ability.timeCounters)} time counters on it. At the beginning of your upkeep, remove a time " +
            "counter. When the last is removed, you may cast it without paying its mana cost."

    private fun wardGloss(ability: KeywordAbility.Ward, self: String): String? {
        val cost = ability.cost
        val payment = when (cost) {
            is com.wingedsheep.sdk.scripting.effects.WardCost.Mana ->
                if (cost.waterbend) return null else "pays ${cost.manaCost}"

            is com.wingedsheep.sdk.scripting.effects.WardCost.Life -> "pays ${cost.amount} life"
            else -> return null
        }
        return "Whenever $self becomes the target of a spell or ability an opponent controls, " +
            "counter it unless that player $payment."
    }

    private fun numericGloss(ability: KeywordAbility.Numeric, self: String): String? {
        val n = ability.n
        return when (ability.keyword) {
            Keyword.CREW -> "Tap any number of creatures you control with total power $n or more: " +
                "This Vehicle becomes an artifact creature until end of turn."

            Keyword.ANNIHILATOR ->
                "Whenever $self attacks, defending player sacrifices ${words(n)} permanents of their choice."

            Keyword.BUSHIDO -> "Whenever this creature blocks or becomes blocked, it gets +$n/+$n until end of turn."

            Keyword.TOXIC -> "Players dealt combat damage by $self also get " +
                (if (n == 1) "a poison counter." else "${words(n)} poison counters.")

            Keyword.MODULAR -> "$self enters with ${words(n)} +1/+1 counters on it. When it dies, you may put its " +
                "+1/+1 counters on target artifact creature."

            Keyword.RENOWN -> "When $self deals combat damage to a player, if it isn't renowned, put " +
                (if (n == 1) "a +1/+1 counter" else "${words(n)} +1/+1 counters") +
                " on it and it becomes renowned."

            else -> null
        }
    }

    private fun simpleGloss(keyword: Keyword, self: String): String? = when (keyword) {
        Keyword.FLYING -> "$self can't be blocked except by creatures with flying or reach."
        Keyword.MENACE -> "$self can't be blocked except by two or more creatures."
        Keyword.INTIMIDATE ->
            "$self can't be blocked except by artifact creatures and/or creatures that share a color with it."

        Keyword.FEAR -> "$self can't be blocked except by artifact creatures and/or black creatures."
        Keyword.SHADOW -> "$self can block or be blocked by only creatures with shadow."
        Keyword.HORSEMANSHIP -> "$self can't be blocked except by creatures with horsemanship."
        Keyword.SWAMPWALK -> landwalkGloss(self, "Swamp")
        Keyword.FORESTWALK -> landwalkGloss(self, "Forest")
        Keyword.ISLANDWALK -> landwalkGloss(self, "Island")
        Keyword.MOUNTAINWALK -> landwalkGloss(self, "Mountain")
        Keyword.PLAINSWALK -> landwalkGloss(self, "Plains")
        Keyword.DESERTWALK -> landwalkGloss(self, "Desert")
        Keyword.FIRST_STRIKE -> "$self deals combat damage before creatures without first strike."
        Keyword.DOUBLE_STRIKE -> "$self deals both first-strike and regular combat damage."
        Keyword.TRAMPLE -> "$self can deal excess combat damage to the player or planeswalker it's attacking."
        Keyword.DEATHTOUCH -> "Any amount of damage this deals to a creature is enough to destroy it."
        Keyword.LIFELINK -> "Damage dealt by $self also causes you to gain that much life."
        Keyword.VIGILANCE -> "Attacking doesn't cause $self to tap."
        Keyword.REACH -> "$self can block creatures with flying."
        Keyword.PROVOKE -> "Whenever $self attacks, you may have target creature defending player controls " +
            "untap and block it if able."

        Keyword.FLANKING -> "Whenever a creature without flanking blocks $self, the blocking creature gets " +
            "-1/-1 until end of turn."

        Keyword.DEFENDER -> "$self can't attack."
        Keyword.INDESTRUCTIBLE -> "Effects that say \"destroy\" don't destroy $self."
        Keyword.HEXPROOF -> "$self can't be the target of spells or abilities your opponents control."
        Keyword.SHROUD -> "$self can't be the target of spells or abilities."
        Keyword.HASTE -> "$self can attack and {T} as soon as it comes under your control."
        Keyword.FLASH -> "You may cast this spell any time you could cast an instant."
        Keyword.PROWESS -> "Whenever you cast a noncreature spell, $self gets +1/+1 until end of turn."
        Keyword.CHANGELING -> "This card is every creature type."
        Keyword.DEVOID -> "This card has no color."
        Keyword.CONVOKE -> "Your creatures can help cast this spell. Each creature you tap while casting this " +
            "spell pays for {1} or one mana of that creature's color."

        Keyword.DELVE -> "Each card you exile from your graveyard while casting this spell pays for {1}."
        Keyword.IMPROVISE -> "Your artifacts can help cast this spell. Each artifact you tap after you're done " +
            "activating mana abilities pays for {1}."

        Keyword.STORM -> "When you cast this spell, copy it for each spell cast before it this turn."
        Keyword.CASCADE -> "When you cast this spell, exile cards from the top of your library until you exile a " +
            "nonland card that costs less. You may cast it without paying its mana cost. Put the exiled cards on " +
            "the bottom in a random order."

        Keyword.REBOUND -> "If you cast this spell from your hand, exile it as it resolves. At the beginning of " +
            "your next upkeep, you may cast this card from exile without paying its mana cost."

        Keyword.SOULBOND -> "You may pair this creature with another unpaired creature when either enters. They " +
            "remain paired for as long as you control both of them."

        Keyword.PERSIST -> "When this creature dies, if it had no -1/-1 counters on it, return it to the " +
            "battlefield under its owner's control with a -1/-1 counter on it."

        Keyword.UNDYING -> "When this creature dies, if it had no +1/+1 counters on it, return it to the " +
            "battlefield under its owner's control with a +1/+1 counter on it."

        Keyword.DECAYED -> "$self can't block. When it attacks, sacrifice it at end of combat."
        Keyword.EXPLOIT -> "When this creature enters, you may sacrifice a creature."
        Keyword.TRAINING -> "Whenever this creature attacks with another creature with greater power, put a " +
            "+1/+1 counter on this creature."

        Keyword.WITHER -> "This deals damage to creatures in the form of -1/-1 counters."
        Keyword.RIOT -> "This creature enters with your choice of a +1/+1 counter or haste."
        Keyword.ASCEND -> "If you control ten or more permanents, you get the city's blessing for the rest of the game."
        Keyword.DAYBOUND -> "If a player casts no spells during their own turn, it becomes night next turn."
        Keyword.NIGHTBOUND -> "If a player casts at least two spells during their own turn, it becomes day next turn."
        Keyword.START_YOUR_ENGINES -> "If you have no speed, it starts at 1. It increases once on each of your " +
            "turns when an opponent loses life. Max speed is 4."

        else -> null
    }

    private fun landwalkGloss(self: String, land: String): String {
        val article = if (land.first() in "AEIOU") "an" else "a"
        return "$self can't be blocked as long as defending player controls $article $land."
    }
}
