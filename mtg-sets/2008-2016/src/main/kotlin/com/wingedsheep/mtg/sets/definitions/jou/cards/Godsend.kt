package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Godsend — Journey into Nyx #12
 * {1}{W}{W} · Legendary Artifact — Equipment · Mythic
 *
 * Equipped creature gets +3/+3.
 * Whenever equipped creature blocks or becomes blocked by one or more creatures, you may exile
 * one of those creatures.
 * Your opponents can't cast spells with the same name as a card exiled with Godsend.
 * Equip {3}
 *
 * "Blocks or becomes blocked by one or more creatures" is the partner-less, once-per-combat
 * `Triggers.attached.blocksOrBecomesBlocked()` (ruling: it triggers only once however many
 * creatures are involved), which captures every partner in [CollectionSlot.TriggerCaptured].
 * "You may exile one of those creatures" chooses up to one of them at resolution — untargeted, so
 * protection from white doesn't stop it (ruling) — narrowed to those still on the battlefield, and
 * links the exiled card to Godsend's exile pile, which Ixalan's Binding's name lock
 * ([PlayersCantCastSpells] over `sharingNameWithLinkedExile()`) reads. A new Godsend has a fresh,
 * empty pile (ruling); the lock covers spells only, never land plays (ruling).
 */
val Godsend = card("Godsend") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Artifact — Equipment"
    oracleText = "Equipped creature gets +3/+3.\n" +
        "Whenever equipped creature blocks or becomes blocked by one or more creatures, you may " +
        "exile one of those creatures.\n" +
        "Your opponents can't cast spells with the same name as a card exiled with Godsend.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(+3, +3, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.blocksOrBecomesBlocked()
        effect = Effects.Pipeline {
            val partners = filter(CollectionSlot.TriggerCaptured, GameObjectFilter.Creature.onBattlefield())
            val chosen = chooseUpTo(
                count = 1,
                from = partners,
                prompt = "You may exile one of those creatures",
                useTargetingUI = true
            )
            move(chosen, CardDestination.ToZone(Zone.EXILE), linkToSource = true)
        }
        description = "Whenever equipped creature blocks or becomes blocked by one or more " +
            "creatures, you may exile one of those creatures."
    }

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.sharingNameWithLinkedExile()
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "12"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2dcf3975-7f81-4bdd-810d-476f46444b7a.jpg?1783939460"

        ruling(
            "2014-04-26",
            "Godsend's triggered ability will trigger only once no matter how many creatures the " +
                "equipped creature blocks or becomes blocked by."
        )
        ruling(
            "2014-04-26",
            "Godsend's triggered ability doesn't target any creature. You could exile a creature with " +
                "protection from white, for example. You choose which creature to exile when the ability resolves."
        )
        ruling(
            "2014-04-26",
            "If Godsend leaves the battlefield, and later another Godsend enters the battlefield, it is a " +
                "new object (even if the two were represented by the same card). The new Godsend won't stop " +
                "opponents from casting cards with the same name as cards exiled with the first one."
        )
        ruling(
            "2014-04-26",
            "If Godsend exiles a land card (perhaps because it was turned into a creature and then blocked " +
                "or was blocked by the equipped creature), opponents can still play land cards with the same " +
                "name as the exiled card. Godsend stops only the casting of spells."
        )
    }
}
