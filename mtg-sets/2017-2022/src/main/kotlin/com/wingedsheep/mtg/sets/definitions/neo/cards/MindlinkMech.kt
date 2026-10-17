package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CopyExceptions
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindlink Mech — Kamigawa: Neon Dynasty #62 (canonical printing)
 * {2}{U} · Artifact — Vehicle · 4/3
 *
 * Flying
 * Whenever this Vehicle becomes crewed for the first time each turn, until end of turn, this
 * Vehicle becomes a copy of target nonlegendary creature that crewed it this turn, except it's 4/3,
 * it's a Vehicle artifact in addition to its other types, and it has flying.
 * Crew 1
 *
 * The copy is [Effects.EachPermanentBecomesCopyOfTarget] with `affected = Self` for
 * [Duration.EndOfTurn]; the exceptions are copiable values (CR 707.9b). The crew effect's "becomes
 * an artifact creature until end of turn" is a non-copy effect, so it keeps applying on top of the
 * copy — which is what leaves a copy of a crewing animated land a 0/0 (the card's ruling).
 */
val MindlinkMech = card("Mindlink Mech") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 3
    oracleText = "Flying\nWhenever this Vehicle becomes crewed for the first time each turn, until " +
        "end of turn, this Vehicle becomes a copy of target nonlegendary creature that crewed it " +
        "this turn, except it's 4/3, it's a Vehicle artifact in addition to its other types, and it " +
        "has flying.\nCrew 1 (Tap any number of creatures you control with total power 1 or more: " +
        "This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.becomesCrewed(firstTimeEachTurn = true)
        val crewer = target(
            TargetFilter(GameObjectFilter.Creature.nonlegendary().crewedOrSaddledSourceThisTurn())
        )
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = crewer,
            duration = Duration.EndOfTurn,
            affected = EffectTarget.Self,
            exceptions = CopyExceptions(
                addedCardTypes = setOf(CardType.ARTIFACT),
                addedSubtypes = setOf(Subtype.VEHICLE),
                addedKeywords = setOf(Keyword.FLYING),
                powerOverride = 4,
                toughnessOverride = 3,
            ),
        )
        description = "Whenever this Vehicle becomes crewed for the first time each turn, until end " +
            "of turn, this Vehicle becomes a copy of target nonlegendary creature that crewed it " +
            "this turn, except it's 4/3, it's a Vehicle artifact in addition to its other types, " +
            "and it has flying."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Robbie Trevino"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28273a5b-57b3-4b7a-b017-5886c171c9c9.jpg?1783923900"
    }
}
