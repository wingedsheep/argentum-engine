package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CardNamePool
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nevermore
 * {1}{W}{W}
 * Enchantment
 * As this enchantment enters, choose a nonland card name.
 * Spells with the chosen name can't be cast.
 *
 * Modeling notes:
 * - The as-enters naming is [EntersWithChoice]`(ChoiceType.CARD_NAME, CardNamePool.NONLAND)`; the
 *   pick is stored durably on the permanent's `CastChoicesComponent` (`ChoiceSlot.CARD_NAME`).
 * - "Spells with the chosen name can't be cast" binds every player, Nevermore's controller
 *   included → [PlayersCantCastSpells]`(Player.Each, …)` filtered by
 *   [GameObjectFilter.namedFromChosenComponent], which reads the name off this permanent (the
 *   cast check passes the granting permanent as the predicate source). Spells already on the stack
 *   and permanents put onto the battlefield without casting are unaffected, per the rulings.
 */
val Nevermore = card("Nevermore") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "As this enchantment enters, choose a nonland card name.\n" +
        "Spells with the chosen name can't be cast."

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.CARD_NAME,
            cardNamePool = CardNamePool.NONLAND,
        )
    )

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.Each,
            spellFilter = GameObjectFilter.Any.namedFromChosenComponent(),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "25"
        artist = "Jason A. Engle"
        flavorText = "\"By the law of Avacyn, the following thoughts, words, and deeds are henceforth disallowed.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67b610fe-36ee-4d58-8ed4-04e7a12587b2.jpg?1783940989"
        ruling("2021-03-19", "If you name one of the faces of a modal double-faced card, players may still play the face you didn't name. (Players can never cast the back face of a transforming double-faced card)")
        ruling("2011-09-22", "No one can cast spells or activate abilities between the time a card is named and the time that Nevermore's ability starts to work.")
        ruling("2011-09-22", "Spells with the chosen name that somehow happen to already be on the stack when Nevermore enters are not affected by Nevermore's ability.")
        ruling("2011-09-22", "Although the named card can't be cast, it can still be put onto the battlefield by a spell or ability (if it's a permanent card).")
        ruling("2011-09-22", "You can name either half of a split card, but not both. If you do so, that half (and both halves, if the split card has fuse) can't be cast. The other half is unaffected.")
        ruling("2011-09-22", "The named card can be cast again once Nevermore leaves the battlefield.")
    }
}
