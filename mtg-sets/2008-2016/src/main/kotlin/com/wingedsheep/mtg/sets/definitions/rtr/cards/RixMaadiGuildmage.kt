package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Rix Maadi Guildmage
 * {B}{R}
 * Creature — Human Shaman
 * 2/2
 * {B}{R}: Target blocking creature gets -1/-1 until end of turn.
 * {B}{R}: Target player who lost life this turn loses 1 life.
 *
 * Canonical printing: Return to Ravnica, the card's only printing.
 *
 * The player restriction is re-checked on resolution (CR 608.2b) through
 * `TargetPlayer.restriction`; the engine-level `PlayerTargetRestrictionScenarioTest` covers it.
 */
val RixMaadiGuildmage = card("Rix Maadi Guildmage") {
    manaCost = "{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "{B}{R}: Target blocking creature gets -1/-1 until end of turn.\n" +
        "{B}{R}: Target player who lost life this turn loses 1 life."

    activatedAbility {
        cost = Costs.Mana("{B}{R}")
        val creature = target(TargetFilter.BlockingCreature)
        effect = Effects.ModifyStats(-1, -1, creature)
    }

    activatedAbility {
        cost = Costs.Mana("{B}{R}")
        val player = target(
            TargetPlayer(
                restriction = Conditions.candidateLostLifeThisTurn(),
                descriptionOverride = "target player who lost life this turn",
            )
        )
        effect = Effects.LoseLife(1, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "192"
        artist = "Karl Kopinski"
        flavorText = "Insatiable, irresistible, and insane."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb737465-5de8-4015-befe-2bf386da2a89.jpg?1783940332"
        ruling(
            "2012-10-01",
            "A blocking creature is one that has been declared as a blocker this combat, or one that was put onto the battlefield blocking this combat. Unless that creature leaves combat, it continues to be a blocking creature through the end of combat step, even if the creature or creatures it was blocking are no longer on the battlefield or have otherwise left combat."
        )
    }
}
