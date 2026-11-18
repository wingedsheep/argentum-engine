package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leeching Sliver
 * {1}{B}
 * Creature — Sliver
 * 1/1
 * Whenever a Sliver you control attacks, defending player loses 1 life.
 *
 * An observer attack trigger over your Slivers (Leeching Sliver itself included) — Campaign of
 * Vengeance's shape. One trigger per attacking Sliver, and each one drains the player *that* Sliver
 * attacks (the controller of the planeswalker it attacks), so Slivers split across opponents drain
 * each of them separately. Like the other Magic 2015 Slivers it only watches *your* Slivers.
 */
val LeechingSliver = card("Leeching Sliver") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Sliver"
    power = 1
    toughness = 1
    oracleText = "Whenever a Sliver you control attacks, defending player loses 1 life."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.SLIVER).youControl()).attacks()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.DefendingPlayer))
        description = "Whenever a Sliver you control attacks, defending player loses 1 life."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Svetlin Velinov"
        flavorText = "\"Seeing one slowly devour one of my party begged the question—do they know what " +
            "cruelty is?\"\n—Hastric, Thunian scout"
        imageUri = "https://cards.scryfall.io/normal/front/8/8/888ca52b-1270-4587-804d-1d08b07d7c5d.jpg?1783939183"
        ruling("2014-07-18", "Slivers in this set affect only Sliver creatures you control. They don't grant bonuses to your opponents' Slivers. This is the same way Slivers from the Magic 2014 core set worked, while Slivers in earlier sets granted bonuses to all Slivers.")
        ruling("2014-07-18", "If you change the creature type of a Sliver you control so it's no longer a Sliver, it will no longer be affected by its own ability. Its ability will continue to affect other Sliver creatures you control.")
    }
}
