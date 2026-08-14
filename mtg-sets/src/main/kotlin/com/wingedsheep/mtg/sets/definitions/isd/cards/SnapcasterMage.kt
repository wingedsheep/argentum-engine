package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Snapcaster Mage
 * {1}{U}
 * Creature — Human Wizard
 * 2/1
 *
 * Flash
 * When this creature enters, target instant or sorcery card in your graveyard gains flashback
 * until end of turn. The flashback cost is equal to its mana cost.
 *
 * The enters trigger targets when it goes on the stack (CR 603.3d) and grants Flashback
 * (CR 702.34) at resolution via [Effects.GrantFlashback]. Leaving `cost` null is what encodes
 * "the flashback cost is equal to its mana cost" — the shared `FlashbackGrants` resolver reads
 * the card's own mana cost, and the cast-from-graveyard enumerator, cast handler, and
 * exile-on-resolution clause honor the grant exactly like a printed flashback. The grant expires
 * in the cleanup step, so an uncast card simply stays in the graveyard.
 *
 * Same shape as Archmage's Newt (OTJ), minus that card's saddled `{0}` branch.
 */
val SnapcasterMage = card("Snapcaster Mage") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "When this creature enters, target instant or sorcery card in your graveyard gains " +
        "flashback until end of turn. The flashback cost is equal to its mana cost. (You may " +
        "cast that card from your graveyard for its flashback cost. Then exile it.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        target = TargetObject(
            filter = TargetFilter.InstantOrSorceryInGraveyard.ownedByYou()
        )
        effect = Effects.GrantFlashback(EffectTarget.ContextTarget(0))
        description = "When this creature enters, target instant or sorcery card in your " +
            "graveyard gains flashback until end of turn. The flashback cost is equal to its " +
            "mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Volkan Baǵa"
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e5b279e-4670-4a1e-87d0-3cab7e4f9e58.jpg?1783940966"
    }
}
