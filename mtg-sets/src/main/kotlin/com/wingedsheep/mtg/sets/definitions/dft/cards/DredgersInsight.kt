package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Dredger's Insight
 * {1}{G}
 * Enchantment
 *
 * Whenever one or more artifact and/or creature cards leave your graveyard, you gain 1 life.
 * When this enchantment enters, mill four cards. You may put an artifact, creature, or land card
 * from among the milled cards into your hand.
 *
 * The first ability is a *batching* trigger ([Triggers.CardsLeaveYourGraveyard]): it fires at most
 * once per batch no matter how many cards left and no matter where they went — cast, exiled,
 * reanimated, returned to hand — so escaping three creature cards at once gains 1 life, not 3.
 *
 * The entry ability selects from the **mill's own collection**, not from the graveyard: only the
 * four cards just milled are eligible, so a creature card that was already sitting in the graveyard
 * can't be grabbed. That is why the pipeline gathers the library top itself (rather than
 * `Patterns.Library.mill(4)` followed by a fresh graveyard gather) — the gathered slot *is* the
 * "from among the milled cards" set. `isMill = true` marks the gather as a mill so mill-replacement
 * effects and "whenever you mill" triggers see it.
 */
val DredgersInsight = card("Dredger's Insight") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever one or more artifact and/or creature cards leave your graveyard, you " +
        "gain 1 life.\n" +
        "When this enchantment enters, mill four cards. You may put an artifact, creature, or land " +
        "card from among the milled cards into your hand. (To mill four cards, put the top four " +
        "cards of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.CardsLeaveYourGraveyard(GameObjectFilter.CreatureOrArtifact)
        effect = Effects.GainLife(1)
        description = "Whenever one or more artifact and/or creature cards leave your graveyard, you gain 1 life."
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Pipeline {
            // "mill four cards"
            val milled = gather(
                CardSource.TopOfLibrary(DynamicAmount.Fixed(4), Player.You, isMill = true)
            )
            toGraveyard(milled)
            // "You may put an artifact, creature, or land card from among the milled cards into your hand."
            val chosen = chooseUpTo(
                1,
                from = milled,
                filter = GameObjectFilter.CreatureOrArtifact or GameObjectFilter.Land,
                showAllCards = true,
                prompt = "You may put an artifact, creature, or land card into your hand",
                selectedLabel = "Put in hand",
                remainderLabel = "Leave in graveyard",
            )
            toHand(chosen)
        }
        description = "When this enchantment enters, mill four cards. You may put an artifact, " +
            "creature, or land card from among the milled cards into your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/1/4/148400a0-7819-4551-9815-9357eed1db4d.jpg?1783907872"
    }
}
