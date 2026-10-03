package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Witch Enchanter {3}{W} // Witch-Blessed Meadow
 * Creature — Human Warlock 2/2
 * When this creature enters, destroy target artifact or enchantment an opponent controls.
 * //
 * Land
 * As this land enters, you may pay 3 life. If you don't, it enters tapped.
 * {T}: Add {W}.
 *
 * The enters trigger is mandatory and targeted — with no artifact or enchantment an opponent
 * controls, it is removed from the stack without effect.
 */
private val WitchEnchanterFront = card("Witch Enchanter") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warlock"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, destroy target artifact or enchantment an opponent controls."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.ArtifactOrEnchantment.opponentControls())
        effect = Effects.Destroy(t)
        description = "When this creature enters, destroy target artifact or enchantment an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Tyler Walpole"
        flavorText = "Shilgengar's famine once starved Stensia into embracing vampirism. The Dawnhart Coven " +
            "would not let the same happen to Kessig."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62061e7c-cf19-4f03-b8fa-2bdba62d6b0b.jpg?1783911236"
    }
}

private val WitchBlessedMeadowBack = card("Witch-Blessed Meadow") {
    typeLine = "Land"
    colorIdentity = "W"
    oracleText = "As this land enters, you may pay 3 life. If you don't, it enters tapped.\n{T}: Add {W}."

    replacementEffect(EntersTapped(payLifeCost = 3))

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "239"
        artist = "Tyler Walpole"
        flavorText = "\"Ghrin-Danu will not let her people go hungry.\"\n—Katilda, Dawnhart Prime"
        imageUri = "https://cards.scryfall.io/normal/back/6/2/62061e7c-cf19-4f03-b8fa-2bdba62d6b0b.jpg?1783911236"
    }
}

val WitchEnchanter: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = WitchEnchanterFront,
    backFace = WitchBlessedMeadowBack,
)
