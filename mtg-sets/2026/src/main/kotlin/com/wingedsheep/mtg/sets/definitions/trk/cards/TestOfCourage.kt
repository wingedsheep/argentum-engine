package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Test of Courage
 * {G}
 * Instant
 *
 * Choose one —
 * • Alter the Past — Exile target card from a graveyard. You gain 2 life.
 * • Obliterate the Transwarp Network — Destroy target artifact or enchantment with mana value 3 or less.
 */
val TestOfCourage = card("Test of Courage") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Alter the Past — Exile target card from a graveyard. You gain 2 life.\n" +
        "• Obliterate the Transwarp Network — Destroy target artifact or enchantment with mana value 3 or less."

    spell {
        modal(chooseCount = 1) {
            mode("Alter the Past — Exile target card from a graveyard. You gain 2 life") {
                val card = target(TargetFilter.CardInGraveyard)
                effect = Effects.Move(card, Zone.EXILE) then Effects.GainLife(2)
            }

            mode("Obliterate the Transwarp Network — Destroy target artifact or enchantment with mana value 3 or less") {
                val permanent = target(TargetFilter.ArtifactOrEnchantment.manaValueAtMost(3))
                effect = Effects.Destroy(permanent)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Zoltan Boros"
        flavorText = "\"Sorry to surprise you. Next time we'll call ahead.\"\n—Captain Kathryn Janeway"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/291b9b6b-54bf-4e71-98f1-cb9301ea0727.jpg?1792668501"
    }
}
