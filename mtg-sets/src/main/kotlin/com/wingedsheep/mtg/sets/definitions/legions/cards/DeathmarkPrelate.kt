package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Deathmark Prelate
 * {3}{B}
 * Creature — Human Cleric
 * 2/3
 * {2}{B}, {T}, Sacrifice a Zombie: Destroy target non-Zombie creature. It can't be regenerated.
 * Activate only as a sorcery.
 */
val DeathmarkPrelate = card("Deathmark Prelate") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 3
    oracleText = "{2}{B}, {T}, Sacrifice a Zombie: Destroy target non-Zombie creature. It can't be regenerated. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Zombie"))
        )
        timing = TimingRule.SorcerySpeed
        val t = target("non-Zombie creature", TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.notSubtype(Subtype("Zombie")))))
        effect = CantBeRegeneratedEffect(t) then
                MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "65"
        artist = "Tony Szczudlo"
        flavorText = "Death is a secret he is willing to share."
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b54fb4b2-ecce-4a6c-8d76-4b5879ba836f.jpg?1562931487"
    }
}
