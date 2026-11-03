package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ManaRestriction

/**
 * Isolinear Processor
 * {2}
 * Artifact
 * {T}: Add {C}. Spend this mana only to cast an artifact spell or activate an ability of an
 *   artifact source.
 * {2}, {T}: Draw a card, then discard a card.
 *
 * Modeling: the restricted mana ability mirrors FIN's Cargo Ship
 * ([ManaRestriction.CardTypeSpellsOrAbilitiesOnly] with both spells and abilities allowed);
 * the loot is [Patterns.Hand.loot].
 */
val IsolinearProcessor = card("Isolinear Processor") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add {C}. Spend this mana only to cast an artifact spell or activate an ability of an artifact source.\n" +
        "{2}, {T}: Draw a card, then discard a card."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(
            1,
            restriction = ManaRestriction.CardTypeSpellsOrAbilitiesOnly(
                cardType = CardType.ARTIFACT,
                allowSpells = true,
                allowAbilities = true,
            ),
        )
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {C}. Spend this mana only to cast an artifact spell or activate an ability of an artifact source."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap)
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "266"
        artist = "David Álvarez"
        imageUri = "https://cards.scryfall.io/normal/front/8/c/8c1a2c13-5e36-4707-9d90-18c2e58a2b26.jpg?1793273907"
    }
}
