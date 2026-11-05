package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dowsing Dagger // Lost Vale (Ixalan — the card's earliest printing)
 * {2}
 * Artifact — Equipment // Land
 *
 * Front — Dowsing Dagger (Artifact — Equipment, {2})
 *   When this Equipment enters, target opponent creates two 0/2 green Plant creature tokens
 *   with defender.
 *   Equipped creature gets +2/+1.
 *   Whenever equipped creature deals combat damage to a player, you may transform this
 *   Equipment.
 *   Equip {2}
 *
 * Back — Lost Vale (Land)
 *   {T}: Add three mana of any one color.
 *
 * Implementation:
 *  - The Plants enter under the targeted opponent's control through [Effects.CreateToken]'s
 *    `controller`, the same spelling as the Hunted cycle (Hunted Troll).
 *  - The flip is Rogue's Gloves' `Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)`
 *    trigger wrapping an optional [Effects.Transform] on Self. Once it is Lost Vale — a land,
 *    no longer an Equipment — the state-based attachment check takes it off the creature, so
 *    the +2/+1 goes with it.
 *  - Lost Vale's mana is [Effects.AddAnyColorMana]`(3)`: one colour chosen, three of it
 *    (Gilded Lotus), not "any combination".
 */

private val DowsingDaggerFront = card("Dowsing Dagger") {
    manaCost = "{2}"
    typeLine = "Artifact — Equipment"
    oracleText = "When this Equipment enters, target opponent creates two 0/2 green Plant " +
        "creature tokens with defender.\n" +
        "Equipped creature gets +2/+1.\n" +
        "Whenever equipped creature deals combat damage to a player, you may transform this " +
        "Equipment.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.CreateToken(
            power = 0,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Plant"),
            keywords = setOf(Keyword.DEFENDER),
            count = 2,
            controller = opponent,
            imageUri = "https://cards.scryfall.io/normal/front/6/4/642d1d93-22d0-43f9-8691-6790876185a0.jpg?1783935809",
        )
        description = "When this Equipment enters, target opponent creates two 0/2 green Plant " +
            "creature tokens with defender."
    }

    staticAbility {
        ability = ModifyStats(2, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.May(Effects.Transform(EffectTarget.Self))
        description = "Whenever equipped creature deals combat damage to a player, you may " +
            "transform this Equipment."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/5/1/514d53be-6ade-4f73-a844-e9ae2dafd6ce.jpg?1783935712"
        ruling(
            "2017-09-29",
            "Attacking with an equipped creature doesn't cause Equipment attached to it to " +
                "become tapped. Dowsing Dagger will normally be untapped when it transforms " +
                "into Lost Vale.",
        )
    }
}

private val LostVale = card("Lost Vale") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "(Transforms from Dowsing Dagger.)\n" +
        "{T}: Add three mana of any one color."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana(3)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add three mana of any one color."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "235"
        artist = "Florian de Gesincourt"
        flavorText = "The pirates had ventured into the interior for treasure. They found " +
            "something far greater."
        imageUri = "https://cards.scryfall.io/normal/back/5/1/514d53be-6ade-4f73-a844-e9ae2dafd6ce.jpg?1783935712"
    }
}

val DowsingDagger: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = DowsingDaggerFront,
    backFace = LostVale,
)
