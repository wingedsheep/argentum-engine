package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Icefall Regent — Dragons of Tarkir #58
 * {3}{U}{U} · Creature — Dragon · 4/3 · Rare
 *
 * Flying
 * When this creature enters, tap target creature an opponent controls. That creature doesn't
 * untap during its controller's untap step for as long as you control this creature.
 * Spells your opponents cast that target this creature cost {2} more to cast.
 *
 * The enters trigger is Dungeon Geists' shape: tap, then a DOESNT_UNTAP grant bounded by
 * [Duration.WhileYouControlSource], which ends for good the moment control of the Regent is lost
 * (the effect is expired, so regaining control doesn't revive it) and keeps following the creature
 * if that creature changes controllers. The tax is Boreal Elemental's
 * [SpellCostTarget.OpponentsCastTargeting] over [GroupFilter.source] — spells only, applied once
 * however many times a spell targets the Regent.
 */
val IcefallRegent = card("Icefall Regent") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Dragon"
    power = 4
    toughness = 3
    oracleText = "Flying\n" +
        "When this creature enters, tap target creature an opponent controls. That creature doesn't " +
        "untap during its controller's untap step for as long as you control this creature.\n" +
        "Spells your opponents cast that target this creature cost {2} more to cast."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.WhileYouControlSource("Icefall Regent"),
            )
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCastTargeting(GroupFilter.source()),
            modification = CostModification.IncreaseGeneric(2),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "58"
        artist = "David Gaillet"
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2cbbd377-4617-4285-b367-98418a588dab.jpg?1783938607"
        ruling("2015-02-25", "Icefall Regent's triggered ability can target any creature controlled by an opponent, including one that is already tapped. In that case, the creature stays tapped and won't untap during its controller's untap step.")
        ruling("2015-02-25", "If another player gains control of Icefall Regent, the creature will no longer be affected by the ability stopping it from untapping, even if you later regain control of Icefall Regent.")
        ruling("2015-02-25", "The ability stopping the creature from untapping will continue to apply to it even if the creature changes controllers.")
        ruling("2015-02-25", "Icefall Regent's last ability affects all spells cast by your opponents that target it, including Aura spells and spells with multiple targets. It doesn't affect abilities.")
    }
}
