package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Animate Dead
 * {1}{B}
 * Enchantment — Aura
 * Enchant creature card in a graveyard
 * When this Aura enters, if it's on the battlefield, it loses "enchant creature card in a graveyard"
 * and gains "enchant creature put onto the battlefield with this Aura." Return enchanted creature
 * card to the battlefield under your control and attach this Aura to it. When this Aura leaves the
 * battlefield, that creature's controller sacrifices it.
 * Enchanted creature gets -1/-0.
 *
 * Cast targeting a creature card in any graveyard, it enters attached to that card — the enchant
 * state-based action leaves it there while the card is still a creature card in a graveyard. The
 * enters trigger gathers the enchanted card, returns it under your control, swaps the enchant
 * ability and attaches (`Effects.EnchantPutOntoBattlefield`), then arms the "leaves the
 * battlefield" sacrifice as a delayed trigger that remembers the returned creature.
 */
val AnimateDead = card("Animate Dead") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature card in a graveyard\n" +
        "When this Aura enters, if it's on the battlefield, it loses \"enchant creature card in a graveyard\" " +
        "and gains \"enchant creature put onto the battlefield with this Aura.\" Return enchanted creature card " +
        "to the battlefield under your control and attach this Aura to it. When this Aura leaves the " +
        "battlefield, that creature's controller sacrifices it.\n" +
        "Enchanted creature gets -1/-0."

    auraTarget = TargetObject(filter = TargetFilter.CreatureInGraveyard)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.SourceInZone(Zone.BATTLEFIELD)
        effect = Effects.Pipeline {
            val returned = gather(
                CardSource.FromZone(Zone.GRAVEYARD, Player.Each, GameObjectFilter.Creature.attachedToBySource())
            )
            move(returned, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
            run(Effects.EnchantPutOntoBattlefield(returned))
            run(
                Effects.CreateDelayedTrigger(
                    trigger = Triggers.self.leaves(),
                    watchedTarget = EffectTarget.Self,
                    fireOnce = true,
                    expiry = DelayedTriggerExpiry.Never,
                    carryCollections = listOf(returned.key),
                    effect = Effects.SacrificeTarget(returned.asTarget, sacrificedByItsController = true)
                )
            )
        }
    }

    staticAbility {
        ability = ModifyStats(-1, 0)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8fd7861d-925f-4b4c-a4ab-60be6f43d50b.jpg?1783948699"
        ruling(
            "2016-06-08",
            "If Animate Dead isn't on the battlefield as its triggered ability resolves, none of its effects " +
                "happen. The creature card won't be returned to the battlefield."
        )
        ruling(
            "2016-06-08",
            "Animate Dead is an Aura, albeit with an unusual enchant ability. You target a creature card in a " +
                "graveyard when you cast it. It enters the battlefield attached to that card. Then it returns that " +
                "card to the battlefield, and attaches itself to the card again (since the card is a new object on " +
                "the battlefield). Animate Dead itself never moves into a graveyard during this process."
        )
        ruling(
            "2016-06-08",
            "Once the creature is returned to the battlefield, Animate Dead can't be attached to anything other " +
                "than it (unless Animate Dead somehow manages to put a different creature onto the battlefield). " +
                "Attempting to move Animate Dead to another creature won't work."
        )
        ruling(
            "2016-06-08",
            "Abilities such as shroud and protection function only on the battlefield unless otherwise " +
                "specified. A creature card with shroud may be targeted by Animate Dead, and Animate Dead will " +
                "become attached to the creature that enters the battlefield."
        )
        ruling(
            "2016-06-08",
            "If the creature put onto the battlefield has protection from black—or if the creature can't " +
                "legally be enchanted by Animate Dead for another reason—Animate Dead won't be able to attach to " +
                "it. It will be put into the graveyard as a state-based action, causing its delayed triggered " +
                "ability to trigger. When the trigger resolves, if the creature's still on the battlefield, its " +
                "controller will sacrifice it."
        )
    }
}
