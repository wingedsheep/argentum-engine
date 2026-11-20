package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Lightning Volley — Born of the Gods #101 (canonical printing)
 * {3}{R}
 * Instant
 * Until end of turn, creatures you control gain "{T}: This creature deals 1 damage to any target."
 *
 * Gruesome Slaughter's shape with Flame Fusillade's pinger: the group is the creatures you control
 * as the spell resolves (CR 611.2c — a resolved ability-granting effect locks in its affected set),
 * so creatures arriving later in the turn gain nothing. Inside the granted ability `Self` is the
 * creature it's on, so that creature taps and is the damage source. Summoning sickness still
 * applies to the {T} cost.
 */
val LightningVolley = card("Lightning Volley") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Until end of turn, creatures you control gain \"{T}: This creature deals 1 damage to any target.\""

    spell {
        effect = Effects.GrantActivatedAbilityToGroup(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = 1,
                    target = anyTarget,
                    damageSource = EffectTarget.Self,
                )
                description = "{T}: This creature deals 1 damage to any target."
            },
            filter = GroupFilter.AllCreaturesYouControl,
            duration = Duration.EndOfTurn,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "101"
        artist = "John Avon"
        flavorText = "\"We are the motes of rain that join to make the thundercloud. The power of the storm " +
            "god surges through us!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/2/b2af8746-033d-4a8a-b62d-ab5addb33d4d.jpg?1783939544"
    }
}
