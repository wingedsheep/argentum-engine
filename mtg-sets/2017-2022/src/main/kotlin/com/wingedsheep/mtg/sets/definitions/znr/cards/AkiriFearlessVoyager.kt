package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Akiri, Fearless Voyager
 * {1}{R}{W}
 * Legendary Creature — Kor Warrior
 * 3/3
 *
 * Whenever you attack a player with one or more equipped creatures, draw a card.
 * {W}: You may unattach an Equipment from a creature you control. If you do, tap that creature and
 * it gains indestructible until end of turn.
 *
 * - The draw is `Triggers.you.attacksAPlayer(with = equipped creature)`: it fires once per player
 *   attacked by at least one equipped creature, and attackers aimed at a planeswalker or battle
 *   don't count — exactly the two rulings.
 * - The Equipment is chosen on resolution, not targeted. The pipeline picks it in two steps: first
 *   an equipped creature you control (choosing none is declining the "you may"), then one Equipment
 *   attached to that creature (auto-picked when there's only one). That is the same set of choices
 *   as picking the Equipment directly, and it keeps "that creature" bound to the former host after
 *   the Equipment comes off. Unattach, tap and the indestructible grant run in one resolution, so no
 *   player can act in between; tapping an already-tapped creature is a no-op and it still gains
 *   indestructible.
 */
val AkiriFearlessVoyager = card("Akiri, Fearless Voyager") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Kor Warrior"
    power = 3
    toughness = 3
    oracleText = "Whenever you attack a player with one or more equipped creatures, draw a card.\n" +
        "{W}: You may unattach an Equipment from a creature you control. If you do, tap that " +
        "creature and it gains indestructible until end of turn."

    triggeredAbility {
        trigger = Triggers.you.attacksAPlayer(with = GameObjectFilter.Creature.youControl().equipped())
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.Pipeline {
            val equippedCreatures = gather(GameObjectFilter.Creature.youControl().equipped())
            val host = chooseUpTo(
                1,
                from = equippedCreatures,
                prompt = "You may choose a creature you control to unattach an Equipment from",
                useTargetingUI = true,
            )
            ifNotEmpty(host) {
                val attached = gather(
                    CardSource.AttachedTo(
                        host = host.asTarget,
                        filter = GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT),
                    )
                )
                val equipment = chooseExactly(
                    1,
                    from = attached,
                    prompt = "Choose an Equipment to unattach",
                    useTargetingUI = true,
                )
                run(Effects.UnattachEquipment(equipment.asTarget))
                run(Effects.Tap(host.asTarget))
                run(Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, host.asTarget, Duration.EndOfTurn))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Ekaterina Burmak"
        flavorText = "\"No angel bars our ascent. Emeria waits above!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69e42511-f653-4a6f-a5d4-50e21dfc8077.jpg?1783929324"
        ruling(
            "2020-09-25",
            "Akiri's first ability has you draw just one card per player you attack with an equipped " +
                "creature, no matter how many equipped creatures you attack them with beyond the first " +
                "and no matter how many Equipment that creature's carrying beyond the first."
        )
        ruling(
            "2023-07-28",
            "Equipped creatures attacking a planeswalker or battle won't cause Akiri's first ability to trigger."
        )
        ruling(
            "2020-09-25",
            "You choose which Equipment to unattach as Akiri's second ability resolves. Unattaching an " +
                "Equipment from a creature won't affect other Equipment attached to that creature. No " +
                "player can take actions between the time you unattach that Equipment and the time that " +
                "the creature becomes tapped and gains indestructible."
        )
        ruling("2020-09-25", "If the creature is already tapped, it just gains indestructible until end of turn.")
    }
}
