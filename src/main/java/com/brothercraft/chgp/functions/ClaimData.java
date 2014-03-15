package com.brothercraft.chgp.functions;

import com.brothercraft.chgp.util.Converters;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCCommandSender;
import com.laytonsmith.annotations.api;
import com.laytonsmith.annotations.startup;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import java.util.ArrayList;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;

public class ClaimData {
    
    private static CArray formPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accesors, ArrayList<String> managers, Target tar) {
		CArray permissions = new CArray(tar);
		CArray Cbuilders = new CArray(tar), Ccontainers = new CArray(tar), Caccesors = new CArray(tar), Cmanagers = new CArray(tar);
		
		for (int i = 0; i < builders.size(); i++)
		    Cbuilders.push(new CString(builders.get(i), tar));
		
		permissions.set("builders", Cbuilders, tar);
		
		for (int i = 0; i < containers.size(); i++)
		    Ccontainers.push(new CString(containers.get(i), tar));
		
		permissions.set("containers", Ccontainers, tar);
		
		for (int i = 0; i < accesors.size(); i++)
		    Caccesors.push(new CString(accesors.get(i), tar));		
		
		permissions.set("accesors", Caccesors, tar);
		
		for (int i = 0; i < managers.size(); i++)
		    Cmanagers.push(new CString(managers.get(i), tar));
		
		permissions.set("managers", Cmanagers, tar);
		
		return(permissions);
    }
    
    @startup
    public static void onEnable(){
	try {
	    Static.checkPlugin("GriefPrevention", Target.UNKNOWN);
	} catch (Exception e) {
	    System.out.println("[CommandHelper] CHGP Could not find GriefPrevention please make sure you have it installed.");
	}
	    System.out.println("[CommandHelper] CHGP Initialized - ACzChef");
    }

	@api
	public static class gp_pclaims extends AbstractFunction {
		public boolean isRestricted() { return true; }
		public Boolean runAsync() { return false; }
		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.CastException, ExceptionType.NotFoundException};
		}

		public Construct exec(Target tar, Environment env, Construct... args) throws ConfigRuntimeException {
			Static.checkPlugin("GriefPrevention", tar);


			GriefPrevention inst = GriefPrevention.instance;

			OfflinePlayer player = null;
			if (args.length == 1) {
				if (args[0] instanceof CString) {
					player = inst.resolvePlayer(args[0].val());
					if (player == null) {
						String msg = "Invalid player name. Please use an exact match.";
						throw new ConfigRuntimeException(msg, ExceptionType.NotFoundException, tar);
					}
				} else {
					String msg = "Expected argument 1 of gp_pclaims to be a string";
					throw new ConfigRuntimeException(msg, ExceptionType.CastException, tar);
				}
			} else {
				MCCommandSender mcs = env.getEnv(CommandHelperEnvironment.class).GetCommandSender();
				player = inst.resolvePlayer(mcs.getName());
			}

			PlayerData playerData = inst.dataStore.getPlayerData(player.getName());

			CArray data = new CArray(tar);
			CArray claimsList = new CArray(tar);
			for (int claim_idx = 0; claim_idx < playerData.claims.size(); claim_idx++) {
				Claim claim = playerData.claims.get(claim_idx);
				CArray claimData = new CArray(tar);
				claimData.set("id", new CInt(claim.getID(), tar), tar);
				claimData.set("area", new CInt(claim.getArea(), tar), tar);

				CArray corners = new CArray(tar);

				MCLocation corner1 = Converters.convertLocation(claim.getLesserBoundaryCorner());
				MCLocation corner2 = Converters.convertLocation(claim.getGreaterBoundaryCorner());

				CArray Ccorner1 = ObjectGenerator.GetGenerator().location(corner1);
				CArray Ccorner2 = ObjectGenerator.GetGenerator().location(corner2);

				Ccorner1.remove(new CString("pitch", tar));
				Ccorner1.remove(new CString("yaw", tar));
				Ccorner2.remove(new CString("pitch", tar));
				Ccorner2.remove(new CString("yaw", tar));

				corners.push(Ccorner1);
				corners.push(Ccorner2);
				claimData.set("corners", corners, tar);

				claimsList.push(claimData);
			}
			CArray claims = new CArray(tar);
			claims.set("list", claimsList, tar);
			claims.set("count", new CInt(claimsList.size(), tar), tar);
			CArray blocks = new CArray(tar);
			blocks.set("accrued", new CInt(playerData.accruedClaimBlocks, tar), tar);
			blocks.set("bonus", new CInt((playerData.bonusClaimBlocks + inst.dataStore.getGroupBonusBlocks(player.getName())), tar), tar);
			blocks.set("total", new CInt((playerData.accruedClaimBlocks + playerData.bonusClaimBlocks + inst.dataStore.getGroupBonusBlocks(player.getName())), tar), tar);
			blocks.set("available", new CInt(playerData.getRemainingClaimBlocks(), tar), tar);

			data.set("claims", claims, tar);
			data.set("blocks", blocks, tar);

			if (!player.isOnline()) {
				inst.dataStore.clearCachedPlayerData(player.getName());
            }

			return data;
		}

		public String getName() {
			return "gp_pclaims";
		}

		public Integer[] numArgs() {
			return new Integer[]{0, 1};
		}

		public String docs() {
			return "array {[player]} Returns the information of all the claims"
				+ " owned by the specified player or the current player if the"
				+ " player was not spefified."
				+ " This will throw a NotFoundException if the player name does not exists.";
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}
	}

    @api
    public static class get_claim_id extends AbstractFunction {

	public ExceptionType[] thrown() {
	    return new ExceptionType[]{ExceptionType.InvalidPluginException, ExceptionType.CastException};
	}

	public boolean isRestricted() {
	    return true;
	}

	public Boolean runAsync() {
	    return false;
	}

	public Construct exec(Target tar, Environment env, Construct... args) throws ConfigRuntimeException {
	    Static.checkPlugin("GriefPrevention", tar);
	    MCLocation l = null;
	    Claim c;
	    
	    if (args[0] instanceof CArray) {
		l = ObjectGenerator.GetGenerator().location(args[0], (l != null ? l.getWorld() : null), tar);
		c = GriefPrevention.instance.dataStore.getClaimAt(Converters.convertLocation(l), true, null);
	    } else {
		 throw new ConfigRuntimeException("Expected argument 1 of get_claim_id to be an array",
		 ExceptionType.CastException, tar);
	    }
	    
	    if(c == null) {
		return new CNull();
	    } else if (c.getID() == null){
		return new CInt(c.parent.getID().intValue(), tar);
	    } else {
		return new CInt(c.getID().intValue(), tar);
	    }
	}

	public String getName() {
	    return "get_claim_id";
	}

	public Integer[] numArgs() {
	    return new Integer[]{1};
	}

	public String docs() {
	    return "int {location} gets the id of a claim at given location";
	}

	public CHVersion since() {
	    return CHVersion.V3_3_1;
	}
    }
    
    @api
    public static class get_claim_info extends AbstractFunction {

	public ExceptionType[] thrown() {
	    return new ExceptionType[]{ExceptionType.InvalidPluginException, ExceptionType.CastException};
	}

	public boolean isRestricted() {
	    return true;
	}

	public Boolean runAsync() {
	    return false;
	}
	
	public Construct exec(Target tar, Environment environment, Construct... args) throws ConfigRuntimeException {
	    Static.checkPlugin("GriefPrevention", tar);
	    MCLocation l = null;
	    Claim c;
	    
	    if (args[0] instanceof CArray) {
		l = ObjectGenerator.GetGenerator().location(args[0], (l != null ? l.getWorld() : null), tar);
		c = GriefPrevention.instance.dataStore.getClaimAt(Converters.convertLocation(l), true, null);
	    } else if(args[0] instanceof CInt) {
		long id = Static.getInt(args[0], tar);
		c = GriefPrevention.instance.dataStore.getClaim(id);
	    } else {
		 throw new ConfigRuntimeException("Expected argument 1 of get_claim_info to be an array or integer.",
		 ExceptionType.CastException, tar);
	    }
	    
	    if(c == null) {
		return new CNull();
	    } 
	    
	    CArray data = new CArray(tar);
	    CArray corners = new CArray(tar);
	    
	    MCLocation corner1 = Converters.convertLocation(c.getLesserBoundaryCorner());
	    MCLocation corner2 = Converters.convertLocation(c.getGreaterBoundaryCorner());
	    
	    CArray Ccorner1 = ObjectGenerator.GetGenerator().location(corner1);
	    CArray Ccorner2 = ObjectGenerator.GetGenerator().location(corner2);
	    
	    for (int i = 0; i <= 5; i++)
		Ccorner1.remove(new CInt(i, tar));
	    
	    for (int i = 0; i <= 5; i++)
		Ccorner2.remove(new CInt(i, tar));
	    
	    Ccorner1.remove(new CString("pitch", tar));
	    Ccorner1.remove(new CString("yaw", tar));
	    Ccorner2.remove(new CString("pitch", tar));
	    Ccorner2.remove(new CString("yaw", tar));
	    
	    corners.push(Ccorner1);
	    corners.push(Ccorner2);
	    
	    data.set("corners", corners, tar);
	    data.set("owner", new CString(c.getOwnerName(), tar), tar);
	    data.set("isadmin", new CBoolean(c.isAdminClaim(), tar), tar);
	    data.set("Height", new CInt(c.getHeight(), tar), tar);
	    
	    if (c.getID() == null) {
		ArrayList<String> builders = new ArrayList<String>(), containers = new ArrayList<String>(), accesors = new ArrayList<String>(), managers = new ArrayList<String>();
		data.set("parentId", new CInt(c.parent.getID().intValue(), tar), tar);
		c.getPermissions(builders, containers, accesors, managers);
		CArray perms = formPermissions(builders, containers, accesors, managers, tar);
		data.set("permissions", perms, tar);
	    } else {
		data.set("id", new CInt(c.getID().intValue(), tar), tar);
		CArray children = new CArray(tar);
		for (int i = 0; i < c.children.size(); i++) {
		    CArray childData = new CArray(tar);
		
		    childData.set("Owner", new CString(c.children.get(i).getOwnerName(), tar), tar);
		    children.push(childData);
		}
		data.set("subclaims", children, tar);
		
		ArrayList<String> builders = new ArrayList<String>(), containers = new ArrayList<String>(), accesors = new ArrayList<String>(), managers = new ArrayList<String>();
		
		c.getPermissions(builders, containers, accesors, managers);
		CArray perms = formPermissions(builders, containers, accesors, managers, tar);
		data.set("permissions", perms, tar);
	    }
	    
	    return data;
	    
	}

	public String getName() {
	    return "get_claim_info";
	}

	public Integer[] numArgs() {
	    return new Integer[] {1};
	}

	public String docs() {
	    return "array {location | id} gets various data about a claim";
	}

	public CHVersion since() {
	    return CHVersion.V3_3_1;
	}
	
    }
}