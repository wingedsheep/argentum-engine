package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * New Prahv Guildmage
 * {W}{U}
 * Creature — Human Wizard
 * 2/2
 * {W}{U}: Target creature gains flying until end of turn.
 * {3}{W}{U}: Detain target nonland permanent an opponent controls. (Until your next turn, that
 * permanent can't attack or block and its activated abilities can't be activated.)
 *
 * Canonical printing: Return to Ravnica, the card's earliest real printing.
 *
 * Detain is spelled out as in Azorius Arrester / Lyev Skyknight.
 */
val NewPrahvGuildmage = card("New Prahv Guildmage") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 2
    oracleText = "{W}{U}: Target creature gains flying until end of turn.\n" +
        "{3}{W}{U}: Detain target nonland permanent an opponent controls. (Until your next turn, that " +
        "permanent can't attack or block and its activated abilities can't be activated.)"

    activatedAbility {
        cost = Costs.Mana("{W}{U}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    activatedAbility {
        cost = Costs.Mana("{3}{W}{U}")
        val permanent = target(TargetFilter.NonlandPermanentOpponentControls)
        effect = Effects.CantAttackOrBlock(permanent, Duration.UntilYourNextTurn) then
            Effects.GrantStaticAbility(
                ability = PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself()),
                target = permanent,
                duration = Duration.UntilYourNextTurn,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Karl Kopinski"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/698b47d1-c72e-4dc3-b28b-7421e0163f22.jpg?1783940335"
        ruling("2013-04-15", "If a noncreature permanent is detained and later turns into a creature, it won't be able to attack or block.")
        ruling(
            "2013-04-15",
            "Activated abilities include a colon and are written in the form \"[cost]: [effect].\" No one can activate any activated abilities, including mana abilities, of a detained permanent."
        )
        ruling("2013-04-15", "The static abilities of a detained permanent still apply. The triggered abilities of a detained permanent can still trigger.")
        ruling("2013-04-15", "If a creature is already attacking or blocking when it's detained, it won't be removed from combat. It will continue to attack or block.")
        ruling("2013-04-15", "If a permanent's activated ability is on the stack when that permanent is detained, the ability will be unaffected.")
        ruling("2013-04-15", "When a player leaves a multiplayer game, any continuous effects with durations that last until that player's next turn or until a specific point in that turn will last until that turn would have begun. They neither expire immediately nor last indefinitely.")
    }
}
