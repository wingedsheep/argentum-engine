package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

// The whole cost is {X}, so "the mana you spent on {X}" is the mana spent to activate the ability.
val IllusionaryMask = card("Illusionary Mask") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{X}: You may choose a creature card in your hand whose mana cost could be paid by some amount of, or all of, the mana you spent on {X}. If you do, you may cast that card face down as a 2/2 creature spell without paying its mana cost. If the creature that spell becomes as it resolves has not been turned face up and would assign or deal damage, be dealt damage, or become tapped, instead it's turned face up and assigns or deals damage, is dealt damage, or becomes tapped. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Mana("{X}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.Pipeline {
            val payable = gather(
                CardSource.FromZone(
                    zone = Zone.HAND,
                    player = Player.You,
                    filter = GameObjectFilter.Creature.manaCostPayableWithManaSpent()
                )
            )
            val chosen = chooseUpTo(
                1,
                from = payable,
                prompt = "You may choose a creature card whose mana cost the mana spent on X could pay",
                selectedLabel = "Choose"
            )
            run(Effects.May(Effects.CastFaceDownFromCollection(chosen, turnsFaceUpInstead = true)))
        }
        description = "Choose a creature card in your hand and cast it face down"
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "249"
        artist = "Amy Weber"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62ef2f37-b8ad-47ad-89ca-d6abcb7ff21b.jpg?1783948666"
        ruling("2009-10-01", "Illusionary Mask's ability will continue to apply to creatures cast face down with it, even if Illusionary Mask has left the battlefield.")
        ruling("2009-10-01", "While the creature card is face down, it's a 2/2 creature with no name, mana cost, color, creature type, abilities, or expansion symbol. Since it has no mana cost, its mana value is 0.")
        ruling("2009-10-01", "You can turn a face-down permanent face up if it would have morph while face up. This applies to creatures you cast face down as a result of Illusionary Mask's effect. The rest of Illusionary Mask's effect applies to it as well.")
        ruling("2009-10-01", "You actually cast the card face-down, much as when playing a spell with Morph. It can be responded to and countered.")
        ruling("2009-10-01", "The effect that turns it face-up is a replacement effect. It doesn't use the stack and can't be responded to.")
        ruling("2009-10-01", "Both the amount and types of mana you spend on {X} are taken into account while you're choosing a creature card from your hand. For example, if you spent {U}{U} on {X}, you can choose a creature card with mana cost {U}{U}, {1}{U}, {2}, or {W/U}{W/U}, among other possibilities, but not one that costs {2}{U} or one that costs {G}.")
        ruling("2004-10-04", "The creature enters face down, so none of its \"enters\" abilities will trigger or have any effect. Also none of its \"As this enters\" abilities apply.")
    }
}
