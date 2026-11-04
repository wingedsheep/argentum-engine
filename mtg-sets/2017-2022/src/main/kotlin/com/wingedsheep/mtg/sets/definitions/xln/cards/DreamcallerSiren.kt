package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dreamcaller Siren
 * {2}{U}{U}
 * Creature — Siren Pirate
 * 3/3
 * Flash
 * Flying
 * This creature can block only creatures with flying.
 * When this creature enters, if you control another Pirate, tap up to two target nonland permanents.
 *
 * "If you control another Pirate" is an intervening-if: checked when the trigger would
 * be put on the stack and again on resolution, per the 2017-09-29 ruling. `excludeSelf` keeps the
 * Siren — itself a Pirate — from satisfying "another". "Up to two target" is the target requirement's
 * own `count`/`optional`; [Effects.TapEachTarget] taps whichever were chosen.
 */
val DreamcallerSiren = card("Dreamcaller Siren") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren Pirate"
    power = 3
    toughness = 3
    oracleText = "Flash\nFlying\nThis creature can block only creatures with flying.\n" +
        "When this creature enters, if you control another Pirate, tap up to two target nonland permanents."

    keywords(Keyword.FLASH, Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouControl(
            GameObjectFilter.Permanent.withSubtype(Subtype.PIRATE),
            excludeSelf = true,
        )
        targets(TargetFilter.NonlandPermanent, count = 2, optional = true)
        effect = Effects.TapEachTarget()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "54"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/5/4/548c92c8-1ae1-48b4-8cbe-aa18f59c1efb.jpg?1783935782"
        ruling(
            "2017-09-29",
            "Dreamcaller Siren's last ability won't trigger if you don't control another Pirate as it " +
                "enters the battlefield. If you lose control of each other Pirate before that ability " +
                "resolves, it won't do anything."
        )
    }
}
