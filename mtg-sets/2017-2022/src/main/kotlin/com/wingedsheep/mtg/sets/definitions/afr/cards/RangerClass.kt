package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.LookAtTopOfLibrary
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ranger Class {1}{G}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * When this Class enters, create a 2/2 green Wolf creature token.
 *
 * {1}{G}: Level 2
 * Whenever you attack, put a +1/+1 counter on target attacking creature.
 *
 * {3}{G}: Level 3
 * You may look at the top card of your library any time.
 * You may cast creature spells from the top of your library.
 *
 * Level 1 is Elturgard Ranger's Wolf; level 2 is the once-per-combat `Triggers.you.attacks()`
 * (Military Intelligence's "whenever you attack") aimed at an attacking creature; level 3 is
 * Vizier of the Menagerie's private peek plus its creature-filtered top-of-library cast permission,
 * both read through the Class's reached level.
 */
val RangerClass = card("Ranger Class") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "When this Class enters, create a 2/2 green Wolf creature token.\n" +
        "{1}{G}: Level 2 — Whenever you attack, put a +1/+1 counter on target attacking creature.\n" +
        "{3}{G}: Level 3 — You may look at the top card of your library any time.\n" +
        "You may cast creature spells from the top of your library."

    // Level 1: a 2/2 green Wolf on entering.
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf")
        )
        description = "When this Class enters, create a 2/2 green Wolf creature token."
    }

    // Level 2: once per combat, a +1/+1 counter on target attacking creature.
    classLevel(2, "{1}{G}") {
        triggeredAbility {
            trigger = Triggers.you.attacks()
            val creature = target(TargetFilter.Creature.attacking())
            effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            description = "Whenever you attack, put a +1/+1 counter on target attacking creature."
        }
    }

    // Level 3: peek at the top card, and cast creature spells from there.
    classLevel(3, "{3}{G}") {
        staticAbility {
            ability = LookAtTopOfLibrary()
        }
        staticAbility {
            ability = CastSpellTypesFromTopOfLibrary(GameObjectFilter.Creature)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "202"
        artist = "Suzanne Helmigh"
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7ca392ca-3219-4694-9a74-aa079c76b91e.jpg?1783926456"
        ruling("2021-07-23", "The triggered ability Ranger Class gets at level 2 can trigger only once per combat, no matter how many creatures you attack with.")
        ruling("2021-07-23", "If Ranger Class is level 3, you can look at the top card of your library whenever you want (with one restriction; see below), even if you don't have priority. This action doesn't use the stack. Knowing what that card is becomes part of the information you have access to, just like you can look at the cards in your hand.")
        ruling("2021-07-23", "If the top card of your library changes while you're casting a spell, playing a land, or activating an ability, you can't look at the new top card until you finish doing so. This means that if you cast a spell from the top of your library, you can't look at the next one until you're done paying for that spell.")
        ruling("2021-07-23", "Ranger Class doesn't change when you can cast creature spells. Normally, this means during your main phase when the stack is empty, although flash may change this.")
        ruling("2021-07-23", "You'll still pay all costs for the spell, including additional costs. You may also pay alternative costs if any are available.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
    }
}
