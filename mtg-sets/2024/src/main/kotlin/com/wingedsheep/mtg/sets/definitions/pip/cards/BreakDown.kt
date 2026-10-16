package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Break Down — Fallout #74
 * {2}{G} · Instant
 *
 * Destroy target artifact or enchantment. Create a Junk token.
 *
 * A green Disenchant with a Junk rider. The Junk token is part of the targeted spell's
 * resolution, so if the target is illegal the spell fizzles and no Junk is created (ruling).
 */
val BreakDown = card("Break Down") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or enchantment. Create a Junk token. (It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. You may play that card this turn. Activate only as a sorcery.\")"

    spell {
        val t = target(TargetFilter.ArtifactOrEnchantment)
        effect = Effects.Destroy(t) then Effects.CreateJunk()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "74"
        artist = "Fesbra"
        flavorText = "\"Old human junk. Worst kind of human junk.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/3389f9d7-b8ae-4c51-90f9-b6385517914a.jpg?1783912399"
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
