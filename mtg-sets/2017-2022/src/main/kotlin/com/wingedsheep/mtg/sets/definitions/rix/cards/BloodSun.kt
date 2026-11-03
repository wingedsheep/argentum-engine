package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Blood Sun
 * {2}{R}
 * Enchantment
 * When this enchantment enters, draw a card.
 * All lands lose all abilities except mana abilities.
 *
 * [LoseAllAbilities] with `exceptManaAbilities`: every land keeps its mana abilities (printed or
 * from a basic land type) and loses everything else. Per the rulings, a land entering while Blood
 * Sun is out has already lost "enters tapped", "as this enters" and its enters triggers (CR 614.12).
 */
val BloodSun = card("Blood Sun") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "When this enchantment enters, draw a card.\nAll lands lose all abilities except mana abilities."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    staticAbility {
        ability = LoseAllAbilities(filter = GroupFilter.AllLands, exceptManaAbilities = true)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Emrah Elmasli"
        flavorText = "As the struggle raged for control of the Immortal Sun, the world grew dark under a blood-red sky."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/875832f4-e541-4c87-8479-731e0eab2cc6.jpg?1783935303"
    }
}
