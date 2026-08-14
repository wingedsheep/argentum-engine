package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Virtue of Persistence // Locthwain Scorn
 * {5}{B}{B}
 * Enchantment
 * At the beginning of your upkeep, put target creature card from a graveyard onto the battlefield
 * under your control.
 *
 * Adventure: Locthwain Scorn — {1}{B}, Sorcery — Adventure
 * Target creature gets -3/-3 until end of turn. You gain 2 life.
 *
 * The upkeep trigger targets [TargetFilter.CreatureInGraveyard] — *a* graveyard, not yours, so it
 * reanimates out of an opponent's yard too — and lands it with
 * [Effects.PutOntoBattlefieldUnderYourControl] so control follows the enchantment's controller
 * rather than the card's owner. It's a targeted trigger, so it simply doesn't go on the stack when
 * every graveyard is empty of creature cards, and it fizzles if the card leaves before resolution.
 *
 * Locthwain Scorn is an ordinary -3/-3 until end of turn plus 2 life; the life gain is not
 * conditional on the creature dying, so it happens even if the target is gone on resolution
 * (the whole spell fizzles only because that is its sole target).
 *
 * Note the front face is an **enchantment**, not a creature — CR 715.3d says only that the controller
 * exiles the Adventure as it resolves and "may play it" from there, with no restriction on the card's
 * type, so this is the same `adventure { }` shape as the creature adventurers and the enchantment is
 * what you cast from exile afterwards.
 */
val VirtueOfPersistence = card("Virtue of Persistence") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, put target creature card from a graveyard onto " +
        "the battlefield under your control."

    triggeredAbility {
        trigger = Triggers.YourUpkeep
        val creatureCard = target(
            "target creature card in a graveyard",
            TargetObject(filter = TargetFilter.CreatureInGraveyard),
        )
        effect = Effects.PutOntoBattlefieldUnderYourControl(creatureCard)
        description = "At the beginning of your upkeep, put target creature card from a graveyard " +
            "onto the battlefield under your control."
    }

    adventure("Locthwain Scorn") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target creature gets -3/-3 until end of turn. You gain 2 life. " +
            "(Then exile this card. You may cast the enchantment later from exile.)"
        spell {
            val victim = target("target creature", Targets.Creature)
            effect = Effects.ModifyStats(-3, -3, victim) then Effects.GainLife(2)
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "115"
        artist = "Piotr Dura"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f1e5cafb-b0e6-4ee5-8c58-6f8e5ef2b9da.jpg?1783915099"

        ruling(
            "2023-09-01",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it " +
                "into its owner's graveyard as it resolves. For as long as it remains exiled, that " +
                "player may cast it as a permanent spell."
        )
        ruling(
            "2023-09-01",
            "You must still follow any timing restrictions and permissions for the permanent spell " +
                "you cast from exile. Normally, you'll be able to cast it only during your main " +
                "phase while the stack is empty."
        )
    }
}
