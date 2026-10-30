package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Ashiok's Erasure
 * {2}{U}{U}
 * Enchantment
 * Flash
 * When this enchantment enters, exile target spell.
 * Your opponents can't cast spells with the same name as the exiled card.
 * When this enchantment leaves the battlefield, return the exiled card to its owner's hand.
 *
 * The enters trigger is Spell Queller's [Effects.ExileTargetSpell] with `linkToSource` — a
 * non-counter exile (so it works on uncounterable spells, per the ruling) that records the card
 * in this enchantment's linked-exile pile. The name lock is Circu, Dimir Lobotomist's
 * [PlayersCantCastSpells] over `sharingNameWithLinkedExile()`, which matches nothing while the
 * pile is empty (a copied spell exiles nothing; the card left exile — rulings). The leaves trigger
 * is Angel of Serenity's [Effects.ReturnLinkedExileToHand]; if the Erasure leaves before its
 * enters trigger resolves, the return finds nothing and the spell stays exiled forever (ruling).
 */
val AshioksErasure = card("Ashiok's Erasure") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Flash\n" +
        "When this enchantment enters, exile target spell.\n" +
        "Your opponents can't cast spells with the same name as the exiled card.\n" +
        "When this enchantment leaves the battlefield, return the exiled card to its owner's hand."

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(TargetFilter.SpellOnStack)
        effect = Effects.ExileTargetSpell(linkToSource = true)
        description = "When this enchantment enters, exile target spell."
    }

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            spellFilter = GameObjectFilter.Any.sharingNameWithLinkedExile()
        )
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileToHand()
        description = "When this enchantment leaves the battlefield, return the exiled card to its owner's hand."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d568c679-8421-4184-a73c-b18c4164fea5.jpg?1783931586"
        ruling("2020-01-24", "The exiled spell isn't countered, but it won't resolve. This works against spells that can't be countered.")
        ruling(
            "2020-01-24",
            "If Ashiok's Erasure leaves the battlefield before its enters-the-battlefield ability has resolved, " +
                "its last ability triggers and resolves with no effect, then its enters-the-battlefield ability " +
                "exiles the target spell for the rest of the game."
        )
        ruling(
            "2020-01-24",
            "If there is no exiled card (perhaps because the exiled spell was a copy of a spell), or if the " +
                "exiled card leaves exile somehow, Ashiok's Erasure won't stop players from casting spells."
        )
        ruling(
            "2020-01-24",
            "If Ashiok's Erasure has exiled a split card, your opponents can't cast a spell with the same name " +
                "as either half of that card."
        )
        ruling(
            "2020-01-24",
            "If Ashiok's Erasure has exiled an adventurer card, your opponents can't cast spells with the same " +
                "name as the creature, but may cast their Adventures."
        )
    }
}
