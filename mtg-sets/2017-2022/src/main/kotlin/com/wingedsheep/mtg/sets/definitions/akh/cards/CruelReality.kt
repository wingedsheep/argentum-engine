package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cruel Reality — Amonkhet #84 (canonical printing)
 * {5}{B}{B} · Enchantment — Aura Curse
 *
 * Enchant player
 * At the beginning of enchanted player's upkeep, that player sacrifices a creature or planeswalker of
 * their choice. If the player can't, they lose 5 life.
 *
 * "If the player can't" is read off the sacrifice itself: the trigger's "sacrificed this way" count
 * starts at zero, so the life loss fires only when nothing was sacrificed — including when the player's
 * only creatures or planeswalkers can't be sacrificed. The player never gets to choose the life loss
 * over a legal sacrifice (the card's ruling).
 */
private const val UPKEEP_TEXT =
    "At the beginning of enchanted player's upkeep, that player sacrifices a creature or planeswalker " +
        "of their choice. If the player can't, they lose 5 life."

val CruelReality = card("Cruel Reality") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura Curse"
    oracleText = "Enchant player\n$UPKEEP_TEXT"

    auraTarget = Targets.Player

    triggeredAbility {
        val enchanted = EffectTarget.PlayerRef(Player.EnchantedPlayer)
        trigger = Triggers.player(Player.EnchantedPlayer).beginningOf(Step.UPKEEP)
        effect = Effects.Sacrifice(GameObjectFilter.CreatureOrPlaneswalker, 1, enchanted) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    DynamicAmounts.permanentsSacrificedThisWay(),
                    ComparisonOperator.EQ,
                    DynamicAmounts.fixed(0),
                ),
                then = Effects.LoseLife(5, enchanted),
            )
        description = UPKEEP_TEXT
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "84"
        artist = "Kieran Yanner"
        flavorText = "As Gideon watched the initiate murder his crop-mate, his admiration of the city of " +
            "Naktamun gave way to horror."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/06a91cf5-4eab-4d9b-90bd-fb933bb00540.jpg?1783936510"
        ruling(
            "2017-04-18",
            "The enchanted player chooses a permanent to sacrifice from among the creatures and planeswalkers " +
                "that player controls. You don't choose which type of permanent the player has to sacrifice."
        )
        ruling(
            "2017-04-18",
            "The enchanted player can't choose to lose 5 life if they have a creature or planeswalker that " +
                "can be sacrificed."
        )
    }
}
