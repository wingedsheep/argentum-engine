package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Rick Jones, Destined Sidekick
 * {G}
 * Legendary Creature — Human Advisor — Uncommon (MSH #184)
 * 0/3
 *
 * "{3}, {T}: Mill four cards. You may put a Hero or enchantment card from among those cards
 *  into your hand."
 *
 * Implementation: same Gather (`isMill = true`) → graveyard → `chooseUpTo(1)` → hand pipeline as
 * Rapid Rescue, since "from among those cards" is again scoped to the four cards this activation
 * milled rather than to the whole graveyard. "Hero card" is a subtype match (Hero is a creature
 * type in this set), so the filter is `Hero or Enchantment`; declining is choosing zero and a mill
 * with no legal pick skips the prompt entirely.
 */
val RickJonesDestinedSidekick = card("Rick Jones, Destined Sidekick") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Advisor"
    power = 0
    toughness = 3
    oracleText = "{3}, {T}: Mill four cards. You may put a Hero or enchantment card from among those cards " +
        "into your hand. (To mill four cards, put the top four cards of your library into your graveyard.)"

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap
        )
        effect = Effects.Pipeline {
            // "Mill four cards."
            val milled = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(4), Player.You, isMill = true),
                name = "milled"
            )
            toGraveyard(milled)
            // "You may put a Hero or enchantment card from among those cards into your hand."
            val kept = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.Any.withSubtype(Subtype.HERO) or GameObjectFilter.Enchantment,
                showAllCards = true,
                prompt = "You may put a Hero or enchantment card from among the milled cards into your hand",
                selectedLabel = "Put into your hand",
                remainderLabel = "Leave in graveyard"
            )
            toHand(kept)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "184"
        artist = "Justyna Dura"
        flavorText = "\"Mar-Vell, did you know I once saved Hulk's life? Hulk, go on. You tell it better than I do.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29bffd7c-73be-4185-976b-36a42a4512fe.jpg?1783902914"
    }
}
