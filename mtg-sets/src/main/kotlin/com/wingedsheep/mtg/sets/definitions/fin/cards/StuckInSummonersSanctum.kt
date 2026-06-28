package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stuck in Summoner's Sanctum
 * {2}{U}
 * Enchantment — Aura
 *
 * Flash
 * Enchant artifact or creature
 * When this Aura enters, tap enchanted permanent.
 * Enchanted permanent doesn't untap during its controller's untap step and its
 * activated abilities can't be activated.
 *
 * Charmed Sleep's lock generalised to any permanent (artifact or creature). The
 * untap restriction reuses [AbilityFlag.DOESNT_UNTAP] on the host (granted via the
 * default `attachedCreature()` group filter, which scopes to any attached permanent).
 * The activation lock uses [PreventActivatedAbilities] scoped with
 * `attachedToBySource()` so it forbids only this Aura's host — the who/when-blind
 * Cursed Totem static read at activation-legality time, narrowed to the enchanted
 * permanent.
 */
val StuckInSummonersSanctum = card("Stuck in Summoner's Sanctum") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\n" +
        "Enchant artifact or creature\n" +
        "When this Aura enters, tap enchanted permanent.\n" +
        "Enchanted permanent doesn't untap during its controller's untap step and its " +
        "activated abilities can't be activated."

    keywords(Keyword.FLASH)

    auraTarget = Targets.CreatureOrArtifact

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Tap(EffectTarget.EnchantedPermanent)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    staticAbility {
        ability = PreventActivatedAbilities(
            filter = GameObjectFilter.Permanent.attachedToBySource(),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Susumu Kuroi"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/6678501e-6349-4e37-ab4c-a31a3d408d52.jpg?1748706046"
    }
}
