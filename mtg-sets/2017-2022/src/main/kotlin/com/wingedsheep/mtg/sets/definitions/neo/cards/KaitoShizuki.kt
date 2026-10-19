package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kaito Shizuki — Kamigawa: Neon Dynasty #226
 * {1}{U}{B} · Legendary Planeswalker — Kaito · Starting loyalty 3
 *
 * At the beginning of your end step, if Kaito entered this turn, he phases out.
 * +1: Draw a card. Then discard a card unless you attacked this turn.
 * −2: Create a 1/1 blue Ninja creature token with "This token can't be blocked."
 * −7: You get an emblem with "Whenever a creature you control deals combat damage to a player,
 *     search your library for a blue or black creature card, put it onto the battlefield, then
 *     shuffle."
 *
 * Modeling notes:
 *  - **The end-step phase-out** is an intervening-if trigger on `SourceEnteredThisTurn`: checked
 *    both when the end step begins and on resolution. Phasing doesn't make Kaito re-enter
 *    (ruling), so he phases back in at your next untap step without the entered-this-turn marker
 *    and the trigger doesn't fire again.
 *  - **The +1's "you attacked this turn"** is the player-level fact (`YouAttackedThisTurn`, you
 *    declared at least one attacker), checked as the ability resolves.
 *  - **The −7 emblem** is a permanent sourceless triggered ability (the Tezzeret / Kaito, Cunning
 *    Infiltrator shape); "a creature you control" reads on the emblem's controller and it fires
 *    once per creature that deals combat damage to a player.
 */
val KaitoShizuki = card("Kaito Shizuki") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Planeswalker — Kaito"
    startingLoyalty = 3
    oracleText = "At the beginning of your end step, if Kaito entered this turn, he phases out.\n" +
        "+1: Draw a card. Then discard a card unless you attacked this turn.\n" +
        "−2: Create a 1/1 blue Ninja creature token with \"This token can't be blocked.\"\n" +
        "−7: You get an emblem with \"Whenever a creature you control deals combat damage to a " +
        "player, search your library for a blue or black creature card, put it onto the battlefield, " +
        "then shuffle.\""

    // At the beginning of your end step, if Kaito entered this turn, he phases out.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.SourceEnteredThisTurn
        effect = Effects.PhaseOut(EffectTarget.Self)
        description = "At the beginning of your end step, if Kaito entered this turn, he phases out."
    }

    // +1: Draw a card. Then discard a card unless you attacked this turn.
    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1) then Effects.If(
            condition = Conditions.Not(Conditions.YouAttackedThisTurn),
            then = Effects.Discard(1),
        )
        description = "Draw a card. Then discard a card unless you attacked this turn."
    }

    // −2: Create a 1/1 blue Ninja creature token with "This token can't be blocked."
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Ninja"),
            staticAbilities = listOf(CantBeBlocked(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/1/4/14eb629e-f09f-4deb-b434-7615a3010a4d.jpg?1783923717",
        )
        description = "Create a 1/1 blue Ninja creature token with \"This token can't be blocked.\""
    }

    // −7: You get an emblem with "Whenever a creature you control deals combat damage to a player,
    //     search your library for a blue or black creature card, put it onto the battlefield, then
    //     shuffle."
    loyaltyAbility(-7) {
        val emblemText = "Whenever a creature you control deals combat damage to a player, search " +
            "your library for a blue or black creature card, put it onto the battlefield, then shuffle."
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = grantedTriggeredAbility {
                trigger = Triggers.a(GameObjectFilter.Creature.youControl())
                    .dealsCombatDamage(Recipient.AnyPlayer)
                effect = Patterns.Library.searchLibrary(
                    filter = GameObjectFilter.Creature.withAnyColor(Color.BLUE, Color.BLACK),
                    destination = SearchDestination.BATTLEFIELD,
                )
                description = emblemText
            },
            descriptionOverride = emblemText,
        )
        description = "You get an emblem with \"$emblemText\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "226"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d35a710-3008-4e27-8afd-6e83329f9917.jpg?1783923832"

        ruling("2022-02-18", "Phased out permanents are treated as though they don't exist. They can't be the target of spells or abilities, their static abilities have no effect on the game, their triggered abilities can't trigger, they can't attack or block, and so on.")
        ruling("2022-02-18", "As Kaito Shizuki phases out, Auras attached to him also phase out at the same time. Those Auras will phase in at the same time Kaito does, and they'll phase in still attached to Kaito.")
        ruling("2022-02-18", "Permanents phase back in during their controller's untap step, immediately before that player untaps their permanents. Kaito phases back in with the same number of loyalty counters that he phased out with.")
        ruling("2022-02-18", "Phasing out doesn't cause permanents to leave or re-enter the battlefield, so no \"leaves the battlefield\" or \"enters the battlefield\" abilities will trigger, and Kaito's first ability won't trigger again at the end of a turn in which he phased in.")
    }
}
