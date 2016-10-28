package cn.crap.controller.front;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import cn.crap.dto.ErrorDto;
import cn.crap.dto.InterfacePDFDto;
import cn.crap.dto.ParamDto;
import cn.crap.dto.ResponseParamDto;
import cn.crap.framework.JsonResult;
import cn.crap.framework.MyException;
import cn.crap.framework.base.BaseController;
import cn.crap.inter.service.table.IInterfaceService;
import cn.crap.inter.service.table.IModuleService;
import cn.crap.inter.service.table.IProjectService;
import cn.crap.inter.service.tool.ICacheService;
import cn.crap.model.Interface;
import cn.crap.model.Module;
import cn.crap.model.Project;
import cn.crap.springbeans.Config;
import cn.crap.utils.Html2Pdf;
import cn.crap.utils.HttpPostGet;
import cn.crap.utils.MyString;
import cn.crap.utils.Page;
import cn.crap.utils.Tools;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Controller("frontInterfaceController")
@RequestMapping("/front/interface")
public class InterfaceController extends BaseController<Interface>{

	@Autowired
	private IInterfaceService interfaceService;
	@Autowired
	private IModuleService moduleService;
	@Autowired
	private ICacheService cacheService;
	@Autowired
	private IProjectService projectService;
	@Autowired
	private Config config;
	
	@RequestMapping("/detail/pdf.do")
	public String pdf(String id, String moduleId) throws Exception {
		try{
			if(MyString.isEmpty(id) && MyString.isEmpty(moduleId)){
				request.setAttribute("result", "参数不能为空，生成PDF失败！");
				return "/WEB-INF/views/result.jsp";
			}
			
			List<InterfacePDFDto> interfaces = new ArrayList<InterfacePDFDto>();
			Interface interFace = null;
			InterfacePDFDto interDto = null;
			Module module = null;
			if( !MyString.isEmpty(id) ){
				interDto= new InterfacePDFDto();
				interFace = interfaceService.get(id);
				if(MyString.isEmpty(interFace.getId())){
					request.setAttribute("result", "接口id有误，生成PDF失败。请确认配置文件config.properties中的网站域名配置是否正确！");
					return "/WEB-INF/views/result.jsp";
				}
				getInterDto(interfaces, interFace, interDto);
			}else{
				module = moduleService.get(moduleId);
				if(MyString.isEmpty(module.getId())){
					request.setAttribute("result", "模块id有误，生成PDF失败。请确认配置文件config.properties中的网站域名配置是否正确！");
					return "/WEB-INF/views/result.jsp";
				}
				for( Interface inter : interfaceService.findByMap(Tools.getMap("moduleId", moduleId), null, null)){
					interDto= new InterfacePDFDto();
					getInterDto(interfaces, inter, interDto);
	
				}
			}
			request.setAttribute("MAIN_COLOR", cacheService.getSetting("MAIN_COLOR").getValue());
			request.setAttribute("ADORN_COLOR", cacheService.getSetting("ADORN_COLOR").getValue());
			request.setAttribute("interfaces", interfaces);
			request.setAttribute("moduleName", interFace == null? module.getName():interFace.getModuleName());
			request.setAttribute("title", cacheService.getSetting("TITLE").getValue());
			return "/WEB-INF/views/interFacePdf.jsp";
		}catch(Exception e){
			request.setAttribute("result", "接口数据有误，请修改接口后再试，错误信息："+e.getMessage());
			return "/WEB-INF/views/result.jsp";
		}
	}

	private void getInterDto(List<InterfacePDFDto> interfaces, Interface interFace, InterfacePDFDto interDto) {
		interDto.setModel(interFace);
		if(interFace.getParam().startsWith("form=")){
			interDto.setFormParams(JSONArray.toArray(JSONArray.fromObject(interFace.getParam().substring(5)),ParamDto.class));
		}else{
			interDto.setCustomParams( interFace.getParam());
		}
		interDto.setTrueMockUrl(config.getDomain()+"/mock/trueExam.do?id="+interFace.getId());
		interDto.setFalseMockUrl(config.getDomain()+"/mock/falseExam.do?id="+interFace.getId());

		interDto.setHeaders( JSONArray.toArray(JSONArray.fromObject(interFace.getHeader()),ParamDto.class));
		interDto.setResponseParam( JSONArray.toArray(JSONArray.fromObject(interFace.getResponseParam()),ResponseParamDto.class) );
		interDto.setErrors( JSONArray.toArray(JSONArray.fromObject(interFace.getErrors()),ErrorDto.class) );
		interfaces.add(interDto);
	}
	
	@RequestMapping("/download/pdf.do")
	@ResponseBody
	public void download(String id,String moduleId,HttpServletRequest req, HttpServletResponse response) throws Exception {
		//interFace = interfaceService.get(interFace.getId());
		String displayFilename = "CrapApi"+System.currentTimeMillis()+".pdf";
        byte[] buf = new byte[1024 * 1024 * 10];  
        int len = 0; 
        ServletOutputStream ut = null;
        BufferedInputStream br = null;  
        response.setHeader("Pragma", "No-cache");  
        response.setHeader("Cache-Control", "must-revalidate, no-transform");  
        response.setDateHeader("Expires", 0L);  
 
        String userAgent = req.getHeader("User-Agent");  
        boolean isIE = (userAgent != null) && (userAgent.toLowerCase().indexOf("msie") != -1); 
        response.setContentType("application/x-download"); 
        if (isIE) {  
            displayFilename = URLEncoder.encode(displayFilename, "UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=\"" + displayFilename + "\"");  
        } else {  
            displayFilename = new String(displayFilename.getBytes("UTF-8"), "ISO8859-1");
            response.setHeader("Content-Disposition", "attachment;filename=" + displayFilename);  
        } 
        br = new BufferedInputStream(new FileInputStream(Html2Pdf.createPdf(req, config, id, moduleId)));
        ut = response.getOutputStream();  
        while ((len = br.read(buf)) != -1)  
             ut.write(buf, 0, len);
        ut.flush();
        br.close();
	}
	
	

	@RequestMapping("/list.do")
	@ResponseBody
	public JsonResult webList(@RequestParam String moduleId,String interfaceName, String url,
			@RequestParam(defaultValue = "1") Integer currentPage,String password,String visitCode) throws MyException{
		if( MyString.isEmpty(moduleId) ){
			throw new MyException("000020");
		}
		
		Page page= new Page(15);
		page.setCurrentPage(currentPage);
		
		// 检查是否需要密码：模块密码>项目密码
		Module module = moduleService.get(moduleId);
		canVisitModule(module, password, visitCode);
			
		List<Interface> interfaces  = interfaceService.findByMap( Tools.getMap("moduleId", moduleId, "interfaceName|like", interfaceName, "fullUrl|like", url), " new Interface(id,moduleId,interfaceName,version,createTime,updateBy,updateTime,remark,sequence)", page, null );
		
		return new JsonResult(1, interfaces, page,
				Tools.getMap("crumbs", Tools.getCrumbs( module.getProjectName(), "#/"+module.getProjectId()+"/module/list", module.getName(), "void") ));
	}

	@RequestMapping("/detail.do")
	@ResponseBody
	public JsonResult webDetail(@ModelAttribute Interface interFace,String password,String visitCode) throws MyException {
		interFace = interfaceService.get(interFace.getId());
		if(interFace!=null){
			
			Module module = cacheService.getModule(interFace.getModuleId());
			Project project = cacheService.getProject(module.getProjectId());
			String needPassword = module.getPassword();
			if(MyString.isEmpty(needPassword)){
				needPassword = project.getPassword();
			}
			canVisit(needPassword, password, visitCode);
			
			/**
			 * 查询相同模块下，相同接口名的其它版本号
			 */
			List<Interface> versions = interfaceService.findByMap(
					Tools.getMap("moduleId",interFace.getModuleId(),"interfaceName",interFace.getInterfaceName(),"version|<>",interFace.getVersion()), null, null);
			return new JsonResult(1, interFace, null, 
					Tools.getMap("versions", versions, 
							"crumbs", 
							Tools.getCrumbs( 
									project.getName(), "#/"+project.getId()+"module/list",
									module.getName()+":接口列表", "#/"+project.getId()+"/interface/list/" + module.getId(),
									interFace.getInterfaceName() , "void"), "module",cacheService.getModule(interFace.getModuleId()) ));
		}else{
			throw new MyException("000012");
		}
	}
	
	@RequestMapping("/debug.do")
	@ResponseBody
	public JsonResult debug(@RequestParam String params, @RequestParam String headers, @RequestParam(defaultValue="") String customParams,
			@RequestParam String debugMethod,@RequestParam String fullUrl) throws Exception {
		
		JSONArray jsonParams = JSONArray.fromObject(params);
		JSONArray jsonHeaders = JSONArray.fromObject(headers);
		Map<String,String> httpParams = new HashMap<String,String>();
		for(int i=0;i<jsonParams.size();i++){
			JSONObject param = jsonParams.getJSONObject(i);
			for(Object paramKey:param.keySet()){
				if(fullUrl.contains("{"+paramKey.toString()+"}")){
					fullUrl = fullUrl.replace("{"+paramKey.toString()+"}", param.getString(paramKey.toString()));
				}else{
					httpParams.put(paramKey.toString(), param.getString(paramKey.toString()));
				}
				
			}
		}
		
		Map<String,String> httpHeaders = new HashMap<String,String>();
		for(int i=0;i<jsonHeaders.size();i++){
			JSONObject param = jsonHeaders.getJSONObject(i);
			for(Object paramKey:param.keySet()){
				httpHeaders.put(paramKey.toString(), param.getString(paramKey.toString()));
			}
		}
		// 如果自定义参数不为空，则表示需要使用post发送自定义包体
		if(!MyString.isEmpty(customParams)){
			return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.postBody(fullUrl, customParams, httpHeaders)));
		}
		
		try{
			switch(debugMethod){
			case "POST":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.post(fullUrl, httpParams, httpHeaders)));
			case "GET":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.get(fullUrl, httpParams, httpHeaders)));
			case "PUT":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.put(fullUrl, httpParams, httpHeaders)));
			case "DELETE":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.delete(fullUrl, httpParams, httpHeaders)));
			case "HEAD":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.head(fullUrl, httpParams, httpHeaders)));
			case "OPTIONS":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.options(fullUrl, httpParams, httpHeaders)));
			case "TRACE":
				return new JsonResult(1, Tools.getMap("debugResult",HttpPostGet.trace(fullUrl, httpParams, httpHeaders)));
			default:
				return new JsonResult(1, Tools.getMap("debugResult","不支持的请求方法："+debugMethod));
		}
		}catch(Exception e){
			return new JsonResult(1, Tools.getMap("debugResult","调试出错\r\nmessage:"+e.getMessage()));
		}
	}

}
