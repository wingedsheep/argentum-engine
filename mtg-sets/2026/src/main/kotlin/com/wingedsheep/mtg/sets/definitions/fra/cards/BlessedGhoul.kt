package com.wingedsheep.mtg.sets.definitions.fra.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val BlessedGhoul = card("Blessed Ghoul") {
    manaCost = "{W/B}"
    colorIdentity = "BW"
    typeLine = "Creature — Zombie Cleric"
    oracleText = "Lifelink\n{2}{W/B}: Return this card from your graveyard to your hand."
    power = 1
    toughness = 1

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Mana("{2}{W/B}")
        effect = Effects.Move(EffectTarget.Self, Zone.HAND, fromZone = Zone.GRAVEYARD)
        activateFromZone = Zone.GRAVEYARD
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Igor Grechanyi"
        flavorText = "He had failed his final exam, but his professor had kindly offered a way to make up the work."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb975803-9bf2-401e-9414-d272df314398.jpg?1789556847"
        inBooster = false
    }
}
