package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cloudform
 * {1}{U}{U}
 * Enchantment
 * When this enchantment enters, it becomes an Aura with enchant creature. Manifest the top card of
 * your library and attach this enchantment to it.
 * Enchanted creature has flying and hexproof.
 *
 * Until its trigger resolves it is a plain enchantment whose "enchanted creature" static touches
 * nothing. The manifest happens even if it has left the battlefield by then; with an empty library
 * it becomes an Aura attached to nothing and the state-based action puts it into the graveyard.
 */
val Cloudform = card("Cloudform") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, it becomes an Aura with enchant creature. Manifest the top " +
        "card of your library and attach this enchantment to it. (To manifest a card, put it onto the " +
        "battlefield face down as a 2/2 creature. Turn it face up any time for its mana cost if it's a " +
        "creature card.)\n" +
        "Enchanted creature has flying and hexproof."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.manifest() then
            Effects.BecomeAura(GameObjectFilter.Creature, attachTo = EffectTarget.PipelineTarget("manifested"))
    }

    staticAbility {
        ability = GrantKeyword(Keyword.FLYING)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HEXPROOF)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "32"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a7bb0f87-a0b3-4389-9580-63db449a2241.jpg?1783938707"
        ruling(
            "2014-11-24",
            "You'll still manifest the top card of your library even if the \"Form\" isn't on the battlefield " +
                "as its enters-the-battlefield ability resolves."
        )
        ruling(
            "2014-11-24",
            "If you have no cards in your library as the ability resolves, the \"Form\" will be put into its " +
                "owner's graveyard as a state-based action."
        )
        ruling("2014-11-24", "If the enchanted creature is turned face up, the \"Form\" will continue to enchant it.")
    }
}
