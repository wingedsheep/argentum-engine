package com.wingedsheep.mtg.sets.definitions.tsp.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stuffy Doll — Time Spiral #264
 * {5} · Artifact Creature — Construct · 0/1
 *
 * Indestructible
 * As this creature enters, choose a player.
 * Whenever this creature is dealt damage, it deals that much damage to the chosen player.
 * {T}: This creature deals 1 damage to itself.
 *
 * The entry choice is [ChoiceType.PLAYER] — any player, its controller included — recorded under
 * `ChoiceSlot.PLAYER` and read back by [Player.ChosenPlayer]. The dealt-damage trigger is Boros
 * Reckoner's shape aimed at that player instead of a target.
 */
val StuffyDoll = card("Stuffy Doll") {
    manaCost = "{5}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Construct"
    power = 0
    toughness = 1
    oracleText = "Indestructible\n" +
        "As this creature enters, choose a player.\n" +
        "Whenever this creature is dealt damage, it deals that much damage to the chosen player.\n" +
        "{T}: This creature deals 1 damage to itself."

    keywords(Keyword.INDESTRUCTIBLE)

    replacementEffect(EntersWithChoice(ChoiceType.PLAYER))

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage()
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), EffectTarget.PlayerRef(Player.ChosenPlayer))
        description = "Whenever this creature is dealt damage, it deals that much damage to the chosen player."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.DealDamage(1, EffectTarget.Self)
        description = "{T}: This creature deals 1 damage to itself."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "264"
        artist = "Dave Allsop"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/14ca7425-a499-4864-b955-369ef2577849.jpg?1783943197"
        ruling(
            "2021-03-19",
            "A creature can be dealt an amount of damage greater than its toughness. For example, if Stuffy Doll " +
                "is dealt 3 damage, its triggered ability deals 3 damage, not 1, to the chosen player."
        )
        ruling(
            "2021-03-19",
            "If your life total is brought to 0 or less at the same time that Stuffy Doll is dealt damage, you " +
                "lose the game before its triggered ability goes on the stack."
        )
    }
}
