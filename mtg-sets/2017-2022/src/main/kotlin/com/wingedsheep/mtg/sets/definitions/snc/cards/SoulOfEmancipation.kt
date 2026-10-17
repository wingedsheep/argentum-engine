package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Soul of Emancipation
 * {4}{G}{W}{U}
 * Creature — Avatar
 * 5/7
 *
 * When this creature enters, destroy up to three other target nonland permanents. For each of
 * those permanents, its controller creates a 3/3 white Angel creature token with flying.
 *
 * The destroy-and-replace runs inside `ForEachTarget`, so each Angel goes to the controller of the
 * permanent it replaces (`TargetController` reads the target being iterated). Targets that became
 * illegal are dropped before resolution and so hand out no Angel, while a legal target that
 * survives the destroy (indestructible, shield counter) still gives its controller one.
 */
val SoulOfEmancipation = card("Soul of Emancipation") {
    manaCost = "{4}{G}{W}{U}"
    colorIdentity = "GWU"
    typeLine = "Creature — Avatar"
    power = 5
    toughness = 7
    oracleText = "When this creature enters, destroy up to three other target nonland permanents. " +
        "For each of those permanents, its controller creates a 3/3 white Angel creature token with flying."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.OtherNonlandPermanent, count = 3, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Destroy(EffectTarget.ContextTarget(0)),
            Effects.CreateToken(
                power = 3,
                toughness = 3,
                colors = setOf(Color.WHITE),
                creatureTypes = setOf("Angel"),
                keywords = setOf(Keyword.FLYING),
                controller = EffectTarget.TargetController,
                imageUri = "https://cards.scryfall.io/normal/front/3/9/39bae778-0dee-47d5-a7cd-f2be11b5e79b.jpg?1783922964"
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "223"
        artist = "Eric Deschamps"
        flavorText = "Light poured from every crack as the sleepers awoke from their long stone slumber."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35b13b82-0650-48f2-9e42-3610b43e2e10.jpg?1783923068"
        ruling(
            "2022-04-29",
            "If a permanent is no longer a legal target as Soul of Emancipation's triggered ability resolves, that target's controller will not create an Angel for that permanent."
        )
        ruling(
            "2022-04-29",
            "If a permanent is still a legal target as Soul of Emancipation's triggered ability resolves but it isn't destroyed, perhaps because it had a shield counter, its controller will create an Angel token for that permanent."
        )
    }
}
