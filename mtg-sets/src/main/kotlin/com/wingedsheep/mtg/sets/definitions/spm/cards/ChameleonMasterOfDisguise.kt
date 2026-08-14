package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Chameleon, Master of Disguise — Marvel's Spider-Man #27
 * {3}{U} · Legendary Creature — Human Shapeshifter Villain · 2/3
 *
 * You may have Chameleon enter as a copy of a creature you control, except his name is
 * Chameleon, Master of Disguise.
 * Mayhem {2}{U}
 */
val ChameleonMasterOfDisguise = card("Chameleon, Master of Disguise") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Shapeshifter Villain"
    power = 2
    toughness = 3
    oracleText = "You may have Chameleon enter as a copy of a creature you control, except his " +
        "name is Chameleon, Master of Disguise.\n" +
        "Mayhem {2}{U} (You may cast this card from your graveyard for {2}{U} if you discarded it " +
        "this turn. Timing rules still apply.)"

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.Creature.youControl(),
            nameOverride = "Chameleon, Master of Disguise"
        )
    )

    mayhem("{2}{U}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "27"
        artist = "Javier Charro"
        flavorText = "\"You have a lovely face. Mind if I borrow it?\""
        imageUri = "https://cards.scryfall.io/normal/front/4/3/43892ce7-f63a-4294-922b-8f879f684033.jpg?1783905356"
    }
}
