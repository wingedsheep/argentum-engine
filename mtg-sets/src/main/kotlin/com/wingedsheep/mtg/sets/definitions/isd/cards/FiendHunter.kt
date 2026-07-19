package com.wingedsheep.mtg.sets.definitions.isd.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Fiend Hunter
 * {1}{W}{W}
 * Creature — Human Cleric
 * 1/3
 *
 * When this creature enters, you may exile another target creature.
 * When this creature leaves the battlefield, return the exiled card to the battlefield
 * under its owner's control.
 *
 * Unlike "exile until ~ leaves the battlefield" wordings, Fiend Hunter uses two separate
 * triggers (CR ruling): if it leaves before the exile trigger resolves, the leaves trigger
 * finds nothing linked and does nothing, then the enter trigger exiles the creature with no
 * pending return — exiling it indefinitely. Modeled here as [Effects.ExileUntilLeaves] (which
 * only exiles + links, it does not auto-return) paired with an explicit leaves-battlefield
 * [Effects.ReturnLinkedExileUnderOwnersControl]. Ordering the leaves trigger before a linked
 * exile exists naturally reproduces the indefinite-exile interaction.
 *
 * "you may exile another target creature" is modeled as an optional target (the player may
 * choose no target to decline); "another" excludes Fiend Hunter itself via
 * [TargetFilter.OtherCreature].
 */
val FiendHunter = card("Fiend Hunter") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, you may exile another target creature.\n" +
        "When this creature leaves the battlefield, return the exiled card to the battlefield under its owner's control."

    // ETB: you may exile another target creature (until this leaves).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target(
            "another target creature",
            TargetCreature(filter = TargetFilter.OtherCreature, optional = true)
        )
        effect = Effects.ExileUntilLeaves(creature)
    }

    // LTB: return the exiled card under its owner's control.
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Wayne Reynolds"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1e4c7d8-11a5-40fe-962b-7e938bf08616.jpg?1783940994"

        ruling(
            "2018-12-07",
            "If Fiend Hunter leaves the battlefield before its first ability has resolved, its second " +
                "ability will trigger and do nothing. Then its first ability will resolve and exile the " +
                "target creature indefinitely. This is different from abilities on other cards that exile " +
                "a permanent \"until\" something happens."
        )
        ruling(
            "2018-12-07",
            "Once the exiled creature returns, it's considered a new object with no relation to the object " +
                "that it was. Auras attached to the exiled creature will be put into their owners' graveyards. " +
                "Equipment attached to the exiled creature will become unattached and remain on the battlefield. " +
                "Any counters on the exiled creature will cease to exist."
        )
        ruling("2018-12-07", "If a token is exiled this way, it won't return to the battlefield.")
    }
}
