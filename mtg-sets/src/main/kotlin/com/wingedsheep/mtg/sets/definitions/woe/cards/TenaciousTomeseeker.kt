package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.bargain
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tenacious Tomeseeker
 * {2}{U}
 * Creature — Human Knight
 * 3/2
 *
 * Bargain (You may sacrifice an artifact, enchantment, or token as you cast this spell.)
 * When this creature enters, if it was bargained, return target instant or sorcery card from your
 * graveyard to your hand.
 *
 * The permanent shape of bargain (CR 702.166b), same as [AgathasChampion] and [HighFaeNegotiator]:
 * the bargained fact is stamped on the spell as it's cast and rides the permanent it becomes, so the
 * enters trigger can still read it. [Conditions.WasBargained] is the intervening-'if' clause
 * (CR 603.4), so an unbargained cast never puts the ability on the stack — and per the WOE ruling you
 * may bargain the spell even when your graveyard holds nothing to return.
 *
 * `InstantOrSorceryInGraveyard.ownedByYou()` is the "from your graveyard" scoping. This also gets the
 * Adventure ruling right for free: an adventurer card in a graveyard is a creature card, not an
 * instant or sorcery card, and `GameObjectFilter.InstantOrSorcery` matches on the front face's types —
 * the deliberately separate `InstantSorceryOrAdventure` filter is what cards that *do* count
 * adventurers (Frantic Firebolt) reach for.
 */
val TenaciousTomeseeker = card("Tenacious Tomeseeker") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Knight"
    power = 3
    toughness = 2
    oracleText = "Bargain (You may sacrifice an artifact, enchantment, or token as you cast this " +
        "spell.)\n" +
        "When this creature enters, if it was bargained, return target instant or sorcery card from " +
        "your graveyard to your hand."

    bargain()

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.WasBargained
        val spellCard = target(
            "target instant or sorcery card from your graveyard",
            TargetObject(filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()),
        )
        effect = Effects.ReturnToHand(spellCard)
        description = "When this creature enters, if it was bargained, return target instant or " +
            "sorcery card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Kai Carpenter"
        flavorText = "\"An intact volume of Tales of the Fae? The king will be pleased.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c40be735-0780-459b-8dd2-a298575beaab.jpg?1783915113"

        ruling(
            "2023-09-01",
            "Adventurer cards aren't instant or sorcery cards while they're in your graveyard. You " +
                "can't use Tenacious Tomeseeker's ability to return an adventurer card from your " +
                "graveyard to your hand."
        )
        ruling(
            "2023-09-01",
            "You may sacrifice only one artifact, enchantment, or token to pay a spell's bargain cost."
        )
        ruling(
            "2023-09-01",
            "You can bargain a permanent spell even if you won't be able to choose targets for an " +
                "enters-the-battlefield ability of that permanent once the spell resolves."
        )
    }
}
