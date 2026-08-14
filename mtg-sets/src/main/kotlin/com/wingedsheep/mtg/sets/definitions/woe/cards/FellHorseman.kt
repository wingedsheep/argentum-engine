package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fell Horseman // Deathly Ride
 * {3}{B}
 * Creature — Zombie Knight
 * 3/3
 * When this creature dies, put it on the bottom of its owner's library.
 *
 * Adventure: Deathly Ride — {1}{B}, Sorcery — Adventure
 * Return target creature card from your graveyard to your hand.
 *
 * The death trigger moves the card from the graveyard, so it only finds the card if it's still
 * there when the trigger resolves (2023-09-01 ruling) — [Effects.PutOnBottomOfLibrary] resolves
 * [EffectTarget.Self] to wherever the card actually is and no-ops if it has since moved on. Same
 * shape as Chaos, the Endless.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the
 * caster cast it as the creature spell while it remains in exile.)
 */
val FellHorseman = card("Fell Horseman") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Knight"
    oracleText = "When this creature dies, put it on the bottom of its owner's library."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.PutOnBottomOfLibrary(EffectTarget.Self)
    }

    adventure("Deathly Ride") {
        manaCost = "{1}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Return target creature card from your graveyard to your hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val t = target("target creature card in your graveyard", Targets.CreatureCardInYourGraveyard)
            effect = Effects.Move(t, Zone.HAND)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "92"
        artist = "Igor Krstic"
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43bb3890-4013-48be-8cb5-54fd8fd8ec52.jpg?1783915106"
        ruling(
            "2023-09-01",
            "Fell Horseman will be put on the bottom of its owner's library only if it's still in " +
                "the graveyard when its ability resolves. If it leaves the graveyard before that point, " +
                "it will stay in whatever zone it's in, even if it's returned to the graveyard before " +
                "the ability resolves."
        )
        ruling(
            "2023-09-01",
            "An adventurer card is a permanent card in every zone except the stack, as well as while " +
                "on the stack if not cast as an Adventure. Ignore its alternative characteristics in " +
                "those cases."
        )
    }
}
