package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Korozda Gorgon — Dragon's Maze #78
 * {3}{B}{G} · Creature — Gorgon · 2/5
 *
 * Deathtouch
 * {2}, Remove a +1/+1 counter from a creature you control: Target creature gets -1/-1 until end of turn.
 *
 * The counter-removal cost is the same shape as Bolrac-Clan Crusher's.
 */
val KorozdaGorgon = card("Korozda Gorgon") {
    manaCost = "{3}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Gorgon"
    power = 2
    toughness = 5
    oracleText = "Deathtouch\n{2}, Remove a +1/+1 counter from a creature you control: Target creature gets -1/-1 until end of turn."

    keywords(Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}"),
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Creature.youControl(), 1)
        )
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(-1, -1, creature)
        description = "{2}, Remove a +1/+1 counter from a creature you control: Target creature gets -1/-1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Volkan Baǵa"
        flavorText = "\"Welcome to my collection.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7006e5b9-d6a3-43ce-904b-b2ac0fea67e5.jpg?1783940028"
    }
}
