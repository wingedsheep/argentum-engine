package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tidal Terror — March of the Machine #82
 * {4}{U}{U} · Creature — Octopus · 5/6
 *
 * Whenever this creature attacks, you may tap two other untapped creatures you control. If you
 * do, this creature can't be blocked this turn.
 * Islandcycling {2}
 *
 * "You may tap two other untapped creatures. If you do, …" is [Effects.MayPay] whose payable cost
 * is the Gather → choose-exactly-2 → Tap pipeline (the Aziza idiom). The gather excludes this
 * creature itself ("other"), which matters when it attacks with vigilance and stays untapped.
 * With fewer than two candidates the cost can't be paid and nothing happens.
 */
val TidalTerror = card("Tidal Terror") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Octopus"
    power = 5
    toughness = 6
    oracleText = "Whenever this creature attacks, you may tap two other untapped creatures you " +
        "control. If you do, this creature can't be blocked this turn.\n" +
        "Islandcycling {2} ({2}, Discard this card: Search your library for an Island card, " +
        "reveal it, put it into your hand, then shuffle.)"

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val tapCost = Effects.Pipeline {
            val tidalTerrorTapPool = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature.untapped(),
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val tidalTerrorToTap = chooseExactly(
                2,
                from = tidalTerrorTapPool,
                prompt = "Tap two other untapped creatures you control",
                useTargetingUI = true
            )
            run(Effects.TapCollection(tidalTerrorToTap, tap = true))
        }
        effect = Effects.MayPay(
            cost = tapCost,
            then = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self),
            descriptionOverride = "You may tap two other untapped creatures you control. If you " +
                "do, this creature can't be blocked this turn.",
        )
    }

    keywordAbility(KeywordAbility.typecycling("Island", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e943948-2326-4446-9a16-64d6040b8856.jpg?1783917022"
    }
}
