package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Crackdown Construct
 * {4}
 * Artifact Creature — Construct
 * 2/2
 * Whenever you activate an ability of an artifact or creature that isn't a mana ability, this
 * creature gets +1/+1 until end of turn.
 *
 * The trigger is the default non-mana form of `activatesAbility`, scoped to an artifact or
 * creature *permanent*: the 2017-02-09 ruling says abilities of artifact or creature cards not on
 * the battlefield don't count, so `.onBattlefield()` keeps a graveyard- or hand-activated ability
 * out. "You activate" — not "you control" — so the source's controller is unrestricted. Crew and
 * equip are activated abilities of the Vehicle / Equipment (artifacts), so they trigger it; a
 * source sacrificed or exiled to pay its own cost is matched as it last existed.
 */
val CrackdownConstruct = card("Crackdown Construct") {
    manaCost = "{4}"
    typeLine = "Artifact Creature — Construct"
    power = 2
    toughness = 2
    oracleText = "Whenever you activate an ability of an artifact or creature that isn't a mana ability, " +
        "this creature gets +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(of = GameObjectFilter.CreatureOrArtifact.onBattlefield())
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
        description = "Whenever you activate an ability of an artifact or creature that isn't a mana ability, " +
            "this creature gets +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "148"
        artist = "Johannes Voss"
        flavorText = "\"All buildings are subject to search. Resistance will be punished.\"\n—Enforcer-Chief Ranaj"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/993bc69a-b615-48c5-af81-252a73384e8c.jpg?1783936730"
        ruling(
            "2017-02-09",
            "Crackdown Construct's ability doesn't trigger if you activate an ability of an artifact or " +
                "creature card not on the battlefield."
        )
        ruling(
            "2017-02-09",
            "An ability whose cost is simply {0} can be activated as many times as you'd like. Each " +
                "activation causes Crackdown Construct's ability to trigger."
        )
    }
}
