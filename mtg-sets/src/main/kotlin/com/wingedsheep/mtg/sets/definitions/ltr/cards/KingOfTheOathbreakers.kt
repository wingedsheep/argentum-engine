package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * King of the Oathbreakers
 * {2}{W}{B}
 * Legendary Creature — Spirit Noble
 * 3/3
 *
 * Flying
 * Whenever King of the Oathbreakers or another Spirit you control becomes the target
 * of a spell, it phases out.
 * Whenever King of the Oathbreakers or another Spirit you control phases in, create a
 * tapped 1/1 white Spirit creature token with flying.
 *
 * Engine notes (Gap 32 — phasing, CR 702.26):
 * - Phasing already exists in the engine: `Effects.PhaseOut` adds a `PhasedOutComponent`,
 *   `GameState.getBattlefield` hides phased-out permanents from projection/combat/targeting,
 *   and `BeginningPhaseManager.performUntapStep` phases them back in (emitting `PhasedInEvent`)
 *   during their controller's untap step.
 * - The "becomes the target of a spell" wording (not abilities) is modeled by the new
 *   `Triggers.BecomesTargetOfSpell(filter)` — a `BecomesTargetEvent(spellsOnly = true)` that
 *   only matches when the targeting source is a spell on the stack (`sourceIsSpell`). The ANY
 *   binding with filter "a Spirit you control" covers both "King … or another Spirit you control"
 *   halves, because King itself is a Spirit you control. `EffectTarget.TriggeringEntity` resolves
 *   to the targeted Spirit, so "it phases out" affects exactly the targeted permanent.
 * - The "phases in" trigger is the new `Triggers.PhasesIn(filter)` over `PhasedInEvent`. King
 *   itself phasing back in (on its controller's untap step) is the common case and is covered by
 *   the same Spirit-you-control filter.
 */
val KingOfTheOathbreakers = card("King of the Oathbreakers") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Creature — Spirit Noble"
    power = 3
    toughness = 3
    oracleText = "Flying\n" +
        "Whenever King of the Oathbreakers or another Spirit you control becomes the target of a " +
        "spell, it phases out. (Treat it and anything attached to it as though they don't exist " +
        "until your next turn.)\n" +
        "Whenever King of the Oathbreakers or another Spirit you control phases in, create a " +
        "tapped 1/1 white Spirit creature token with flying."

    keywords(Keyword.FLYING)

    // Whenever King of the Oathbreakers or another Spirit you control becomes the target
    // of a spell, it phases out.
    triggeredAbility {
        trigger = Triggers.BecomesTargetOfSpell(
            GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT).youControl()
        )
        effect = Effects.PhaseOut(EffectTarget.TriggeringEntity)
    }

    // Whenever King of the Oathbreakers or another Spirit you control phases in, create a
    // tapped 1/1 white Spirit creature token with flying.
    triggeredAbility {
        trigger = Triggers.PhasesIn(
            GameObjectFilter.Creature.withSubtype(Subtype.SPIRIT).youControl()
        )
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
            tapped = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Tatiana Veryayskaya"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc857755-62e6-4d29-95f5-82f5d4bde522.jpg?1686969851"
    }
}
