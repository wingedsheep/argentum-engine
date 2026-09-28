package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Invasion of Karsus // Refraction Elemental — March of the Machine #146.
 * {2}{R}{R} · Battle — Siege · defense 4 // Creature — Elemental 4/4
 *
 * The front is a sweep over creatures and planeswalkers (the Siege is a battle, so it is spared).
 * The back's "Ward—Pay 2 life" is [WardCost.Life]; its cast trigger pings each opponent.
 */
private val InvasionOfKarsusFront = card("Invasion of Karsus") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Battle — Siege"
    startingDefense = 4
    oracleText = "(As a Siege enters, choose an opponent to protect it. You and others can attack " +
        "it. When it's defeated, exile it, then cast it transformed.)\n" +
        "When this Siege enters, it deals 3 damage to each creature and each planeswalker."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.dealDamageToAll(3, GroupFilter(GameObjectFilter.CreatureOrPlaneswalker))
        description = "When this Siege enters, it deals 3 damage to each creature and each planeswalker."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/7/7/776b8951-cf15-47ff-b36f-f47706eddb6f.jpg?1783916995"
    }
}

private val RefractionElemental = card("Refraction Elemental") {
    manaCost = ""
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Ward—Pay 2 life.\nWhenever you cast a spell, this creature deals 2 damage to each opponent."

    keywordAbility(KeywordAbility.Ward(WardCost.Life(2)))

    triggeredAbility {
        trigger = Triggers.you.casts()
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Zoltan Boros"
        flavorText = "The crystal landslide stirred over the shattered forms of the Machine Legion vanguard, and then it rose and rearranged itself into an avenging colossus."
        imageUri = "https://cards.scryfall.io/normal/back/7/7/776b8951-cf15-47ff-b36f-f47706eddb6f.jpg?1783916995"
    }
}

val InvasionOfKarsus: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = InvasionOfKarsusFront,
    backFace = RefractionElemental,
)
