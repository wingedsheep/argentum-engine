package com.wingedsheep.mtg.sets.definitions.tor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Putrid Imp — Torment #77
 * {B} · Creature — Zombie Imp · 1/1
 *
 * Discard a card: This creature gains flying until end of turn.
 * Threshold — As long as there are seven or more cards in your graveyard, this creature gets
 * +1/+1 and can't block.
 *
 * Threshold is an ability word: both halves are self-scoped statics behind the same
 * [Conditions.CardsInGraveyardAtLeast] gate.
 */
val PutridImp = card("Putrid Imp") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Imp"
    power = 1
    toughness = 1
    oracleText = "Discard a card: This creature gains flying until end of turn.\n" +
        "Threshold — As long as there are seven or more cards in your graveyard, this creature gets +1/+1 " +
        "and can't block."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7),
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBlock(GroupFilter.source()),
            condition = Conditions.CardsInGraveyardAtLeast(7),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Wayne England"
        imageUri = "https://cards.scryfall.io/normal/front/1/b/1b9e6c5c-4bc4-4f41-8c2c-f5b8c97c53c5.jpg?1783945153"
    }
}
