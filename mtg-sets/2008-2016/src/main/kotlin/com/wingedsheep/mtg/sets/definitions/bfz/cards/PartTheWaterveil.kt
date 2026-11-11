package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Part the Waterveil — Battle for Zendikar #80 (canonical printing)
 * {4}{U}{U} · Sorcery
 *
 * Take an extra turn after this one. Exile Part the Waterveil.
 * Awaken 6—{6}{U}{U}{U}
 *
 * Temporal Mastery's body ([Effects.TakeExtraTurn] + `selfExile()`) behind Coastal Discovery's
 * awaken. The self-exile is a resolution flag on the spell script, so it holds for the awakened
 * cast too; a spell that doesn't resolve (awakened, with its only target gone) goes to the
 * graveyard instead (ruling).
 */
val PartTheWaterveil = card("Part the Waterveil") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Take an extra turn after this one. Exile Part the Waterveil.\n" +
        "Awaken 6—{6}{U}{U}{U} (If you cast this spell for {6}{U}{U}{U}, also put six +1/+1 counters " +
        "on target land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(6, "{6}{U}{U}{U}"))

    spell {
        selfExile()
        effect = Effects.TakeExtraTurn()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "80"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/8/3/83fe992f-0441-42be-89f9-2387b4bc24ce.jpg?1783938208"
        ruling("2015-08-25", "Exiling Part the Waterveil is part of its effect. If Part the Waterveil doesn't resolve, including if it was cast for its awaken cost and the land you control became an illegal target, it will be put into its owner's graveyard.")
        ruling("2015-08-25", "You can cast a spell with awaken for its mana cost and get only its first effect. If you cast a spell for its awaken cost, you'll get both effects.")
    }
}
